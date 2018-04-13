(ns slack-rtm
  (:require [cheshire.core :refer [generate-string parse-string]]
            [clj-http.client :as http]
            [clojure.core.async :as async :refer [go-loop]]
            [gniazdo.core :as ws]
            [throttler.core :refer [throttle-chan]]
            [clojure.core.match :refer [match]]
            [clojure.string :as s]
            c)
  (:use [clojure.algo.generic.functor :only [fmap]]))

(def api-socket-url "https://slack.com/api/")

(defn run-api-get
  ([api-token method args]
   (let [response (-> (http/get (str api-socket-url method)
                                {:query-params (merge {:token      api-token
                                                       :no_unreads true}
                                                      args)
                                 :as :json})
                      :body)
         ;;_ (when (not=  "rtm.start" method)(c/intln "DEBUG" (pr-str args response)))
         ]
     (when-not (:ok response)
       (c/intln "ERROR3:" response))
     (when (:ok response)
       response)))
  ([api-token method] (run-api-get api-token method {})))

(defn run-api-post
  [api-token method args file-content]
  (let [response (-> (http/post (str api-socket-url method)
                                {:query-params (merge {:token      api-token
                                                       :no_unreads true}
                                                      args)
                                 :as :json
                                 :multipart [{:name "file" :content file-content}]})
                     :body)]
    (when-not (:ok response)
      (c/intln "ERROR4:" response))
    (when (:ok response)
      response)));;TODO remove this !!!!

(defn get-websocket-url [api-token]
  (let [rtm-start (run-api-get api-token "rtm.start")]
    [(:url rtm-start) (:self rtm-start) (:users rtm-start)]))

(defn in?
  "true if seq contains elm"
  [seq elm]
  (some #(= elm %) seq))

(defn member-of-im [api-token] (-> (run-api-get api-token "auth.test") :user_id))

(defn member-of-groups [api-token] (->> (run-api-get api-token "groups.list") :groups))

(defn member-of-channels [api-token]
  (let [me (:user_id (run-api-get api-token "auth.test"))
        ch (run-api-get api-token "channels.list")]
    (->> ch :channels (filter #(in? (:members %) me)))))

(defn member-of [api-token] (->> [member-of-im member-of-groups member-of-channels] (map #(% api-token)) (mapcat :id)))

(def buf-size (* 8 1024))

(defn chat-update [api-token ts channel text]
  (run-api-get api-token "chat.update" {:ts ts :channel channel :text text}))

(defn chat-delete [api-token ts channel]
  (run-api-get api-token "chat.delete" {:ts ts :channel channel}))

(defn user-info [api-token user]
  (run-api-get api-token "users.info" {:user user}))

(defn chat-upload [api-token channels file-name file-content]
  (run-api-post api-token "files.upload" {:channels (s/join "," channels) :filename file-name} file-content))


(defn connect-socket [url throttle-params]
  (let [in (async/chan)
        out (async/chan buf-size)
        tout (apply throttle-chan out throttle-params)
        socket (ws/connect
                 url
                 :on-receive
                 (fn [m]
                   (async/put! in (parse-string m true)))
                 :on-error
                 (fn [e]
                   (c/intln "ERROR1:" e)
                   (flush)
                   (async/close! in))
                 :on-close
                 (fn [sc reason]
                   (c/intln "CLOSED:" sc reason)
                   (flush)
                   (async/close! in))
                 )]
    (go-loop []
      (let [[ts m] (async/<! tout)
            _ (c/intln ":: outcomming >>>>" (pr-str m))
            delay (- (System/currentTimeMillis) ts)
            sorry-prefix (when (> delay 2999) (c/intln "WARNING: message rate throttling kicked in:" delay) "_Sorry for the delay, clover has been unusually busy_\n")
            s (generate-string (update-in m [:text] (partial str sorry-prefix)))]
        (ws/send-msg socket s)
        (recur)))
    [in out socket]))


(defn- fix-input [team-id rtm]
  (let [fix1 (if (-> rtm :type (= "message"))
               (assoc rtm :subtype (:subtype rtm));;nil over absence
               rtm)
        fix2 (if (-> rtm :message)
               (assoc-in fix1 [:message :subtype] (-> rtm :message :subtype))
               fix1)]
    (assoc fix2 :team (or (:team fix2) team-id))))

(defn- add-user-info[users-map rtm path]
  (let [user (get-in rtm path)
        user-info (users-map user)]
    (merge rtm (select-keys user-info [:name :real_name]))))

(defn- fix-input-accepted?[team-id add-user-info-f rtm]
  (when (or (-> rtm :type (= "message")) (:ok rtm));;perhaps unnecessary - optimization
    (let [fixed-rtm (fix-input team-id rtm)]
      (match [fixed-rtm];;fix/add non exisitng :subtype, makes matching easier
             [{:type "message" :subtype nil}] (add-user-info-f fixed-rtm [:user])
             [{:type "message" :subtype "message_changed" :message {:subtype nil}}] (add-user-info-f fixed-rtm [:message :user])
             [{:type "message" :subtype "message_deleted"}] fixed-rtm
             [{:ok _ :reply_to _ :ts _}] fixed-rtm
             :else nil))))

(defn start [{:keys [api-token throttle-params team-id]}]
  (let [cin (async/chan buf-size)
        cout (async/chan buf-size)
        _ (c/intln ":: start:1")
        [url self users] (get-websocket-url api-token)
        _ (c/intln ":: start:2")
        users-map (->> users (group-by :id) (fmap first))
        self-send (fn[m] ((hash-set (-> m :user) (-> m :message :user) (-> m :previous_message :user)) (:id self)))
        counter (atom 0)
        ack-map (atom {})
        next-id (fn [] (swap! counter inc))
        shutdown (fn []
                   (async/close! cin)
                   (async/close! cout))]
    (when (clojure.string/blank? url)
      (c/intln ":: start:Could not get RTM Websocket URL")
      (throw (ex-info "Could not get RTM Websocket URL" {})))

    (c/intln ":: got websocket url:" url)

    ;; start a loop to process messages
    (go-loop [[in out socket] (connect-socket url throttle-params)]
      ;; get whatever needs to be done for either data coming from the socket
      ;; or from the user
      (let [[v p] (async/alts! [cout in])]
        ;; if something goes wrong, just die for now
        ;; we should do something smarter, may be try and reconnect
        (if (nil? v)
          (do
            (c/intln "A channel returned nil, may be its dead? Leaving loop.")
            (ws/close socket)
            (flush)
            (shutdown))
          (do
            (if (= p cout)
              (condp = (:c-dispatch v)
                :c-ackpost (let [nid (next-id)
                             context (:c-context v)
                             payload {:channel (:c-channel v) :text (:c-text v)}
                             m (assoc payload :id nid :type "message")]
                         (swap! ack-map assoc nid {:c-context context :c-payload m})
                         (async/>! out [(System/currentTimeMillis) m]))
                :c-post (let [nid (next-id) ;;
                                m {:id nid :type "message" :channel (:c-channel v) :text (:c-text v)}]
                            (async/>! out [(System/currentTimeMillis) m]))
                :c-update (chat-update api-token (:c-ts v) (:c-channel v) (:c-text v))
                :c-delete (chat-delete api-token (:c-ts v) (:c-channel v))
                nil)

              (when-let[vv (fix-input-accepted? team-id (partial add-user-info users-map) v)]
                (let [rto (vv :reply_to)
                      #_(c/intln ":: incomming <<<<:" (pr-str v))]
                  (if-not rto
                    (if-not (self-send vv)
                      (async/>! cin vv)
                      #_(c/intln "IGNORING:" (pr-str v)))
                    (if-let [m (@ack-map rto)]
                      (do
                        (async/>! cin (assoc m :type "message" :subtype "message_sent" :reply vv))
                        (swap! ack-map dissoc rto))
                      #_(c/intln "ERROR2:" (pr-str rto))
                      ))))
              )
            (recur [in out socket]))
          )))
    [cin cout shutdown]))
