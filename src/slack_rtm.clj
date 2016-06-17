(ns slack-rtm
  (:require [cheshire.core :refer [generate-string parse-string]]
            [clj-http.client :as http]
            [clojure.core.async :as async :refer [go-loop]]
            [gniazdo.core :as ws]
            [throttler.core :refer [throttle-chan]]))

(def api-socket-url "https://slack.com/api/")

(defn run-api [api-token method]
  (let [response (-> (http/get (str api-socket-url method)
                               {:query-params {:token      api-token
                                               :no_unreads true}
                                :as :json})
                     :body)]
    (when (:ok response)
      response)))

(defn get-websocket-url [api-token] (:url (run-api api-token "rtm.start")))

(defn in?
  "true if seq contains elm"
  [seq elm]
  (some #(= elm %) seq))

(defn member-of [api-token]
  (let [me (:user_id (run-api api-token "auth.test"))
        im (run-api api-token "im.list")
        gr (run-api api-token "groups.list")
        ch (run-api api-token "channels.list")
        all (concat (->> im :ims) (->> gr :groups) (->> ch :channels (filter #(in? (:members %) me))))]
    (map :id all)))

(def buf-size (* 8 1024))

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
                   (println "ERROR:" e)
                   (flush)
                   (async/close! in))
                 :on-close
                 (fn [sc reason]
                   (println "CLOSED:" sc reason)
                   (flush)
                   (async/close! in))
                 )]
    (go-loop []
      (let [[ts m] (async/<! tout)
            delay (- (System/currentTimeMillis) ts)
            sorry-prefix (when (> delay 999) (println "WARNING: message rate throttling kicked in:" delay) "_Sorry for the delay, clover has been unusually busy_\n")
            s (generate-string (update-in m [:text] (partial str sorry-prefix)))]
        (ws/send-msg socket s)
        (recur)))
    [in out]))

(defn start [{:keys [api-token throttle-params]}]
  (let [cin (async/chan buf-size)
        cout (async/chan buf-size)
        url (get-websocket-url api-token)
        counter (atom 0)
        next-id (fn []
                  (swap! counter inc))
        shutdown (fn []
                   (async/close! cin)
                   (async/close! cout))]
    (when (clojure.string/blank? url)
      (throw (ex-info "Could not get RTM Websocket URL" {})))

    (println ":: got websocket url:" url)

    ;; start a loop to process messages
    (go-loop [[in out] (connect-socket url throttle-params)]
      ;; get whatever needs to be done for either data coming from the socket
      ;; or from the user
      (let [[v p] (async/alts! [cout in])]
        ;; if something goes wrong, just die for now
        ;; we should do something smarter, may be try and reconnect
        (if (nil? v)
          (do
            (println "A channel returned nil, may be its dead? Leaving loop.")
            (flush)
            (shutdown))
          (do
            (if (= p cout)
              (async/>! out [(System/currentTimeMillis) (assoc v :id (next-id) :type "message")])
              (do
                #_(println ":: incoming:" v)
                (when-not (contains? v :reply_to);;## ignore own messages to prevent loops
                  (async/>! cin {:input (:text v) :meta  v}))))
            (recur [in out])))))
    [cin cout shutdown]))

