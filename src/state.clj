(ns state
  (:require
   [clojure.core.cache :as ca]
   [clojure.java.io :as io]
   dialogs
   db
   lang
   evaluator
   persist
   cache))

;;
;;
;;
(defmulti eval-parsed-event!
  (fn [parsed-event]
    (when (-> parsed-event :command (not= :none))
        (persist/log-use parsed-event))
    (:command parsed-event)))

(defmethod eval-parsed-event! :explain [parsed-event]
  (let [t (-> parsed-event :args :term)
        l (db/lookup t)]
    (if l
      {:c-text (lang/format-lookup t l (-> parsed-event :metag))}
      {:c-disposition :c-not-found
       :c-text (lang/format-not-found t)})))

(defmethod eval-parsed-event! :research [parsed-event]
  (let [t (-> parsed-event :args :query)]
    {:c-text (lang/format-explain t "WIP" (-> parsed-event :metag))}
    ))

(defmethod eval-parsed-event! :help [parsed-event]
  {:c-text lang/format-help})

(defmethod eval-parsed-event! :evaluate [parsed-event]
  {:c-text (evaluator/evaluate (-> parsed-event :args :expression))})

(defmethod eval-parsed-event! :define [parsed-event]
  (persist/store parsed-event)
  (db/teach (-> parsed-event :args :term) (:definition (-> parsed-event :args)))
  {:c-disposition :c-new-definition
   :c-text (lang/format-thx (-> parsed-event :args :term) (-> parsed-event :metag))})

(defmethod eval-parsed-event! :none [parsed-event]
  {:c-text "_it seems that you changed your mind or made a typo so this is not clover request any more, this will disappear when you delete your original request_"})


;;
;;
;;

(defn- slack-unique-id-selector [rtm-event] (mapv rtm-event [:type :subtype]))
(defmulti slack-unique-id slack-unique-id-selector)

(defmethod slack-unique-id ["message" nil] [rtm-event]
  (select-keys rtm-event [:user :ts :team]))

(defmethod slack-unique-id ["message" "message_sent"] [rtm-event]
  (let [{context :c-context payload :c-payload} rtm-event]
    (slack-unique-id (:rtm-event context))))

(defmethod slack-unique-id ["message" "message_changed"] [rtm-event]
  (let [{message :message} rtm-event]
    (assoc (select-keys  message [:user :ts]) :team (:team rtm-event))))

(defmethod slack-unique-id ["message" "message_deleted"] [rtm-event]
  {:user (-> rtm-event :previous_message :user) :ts (:deleted_ts rtm-event) :team (:team rtm-event)})

(defmethod slack-unique-id :default [rtm-event] nil)

(defmulti process-slack-message! (fn[fsm-id rtm-event] (slack-unique-id-selector rtm-event)))

(defmethod process-slack-message! ["message" nil] [fsm-id rtm-event]
  (when-let [parsed (lang/parse (:text rtm-event))]
    (when-not (= :none (:command parsed))
        (let [parsed-event (assoc parsed :rtm-event rtm-event)]
          [true
           (assoc parsed-event :c-evaled-event (eval-parsed-event! parsed-event) :c-fsm-id fsm-id)]))))

(defmethod process-slack-message! ["message" "message_sent"] [fsm-id rtm-event]
  [false
   (assoc {:ack :ok} :rtm-event rtm-event :c-fsm-id fsm-id)])

(defmethod process-slack-message! ["message" "message_changed"] [fsm-id rtm-event]
  (let [{message :message} rtm-event]
    (when-let [parsed (lang/parse (:text message))]
      (let [parsed-event (assoc parsed :rtm-event rtm-event)]
        [(not= :none (:command parsed))
         (assoc parsed-event :c-evaled-event (eval-parsed-event! parsed-event) :c-fsm-id fsm-id)]))))

(defmethod process-slack-message! ["message" "message_deleted"] [fsm-id rtm-event]
  [false
   {:command :delete :rtm-event rtm-event :c-fsm-id fsm-id}])

(defmethod process-slack-message! :default [fsm-id rtm-event] nil)

;;
;;
;;
(def fsm-dir (str (:db config/config) "/dialogs/"))

(defn- fsm-file [fsm-key] (io/as-file (str fsm-dir (:ts fsm-key) "-" (:team fsm-key) "-" (:user fsm-key))))

(defn- save-fsm [fsm-key c-fsm]
  (let [fsm-file (fsm-file fsm-key)
        content (with-out-str (pr [(:state @c-fsm) (:value @c-fsm)]))]
    (spit fsm-file content)))

(defn- load-fsm [fsm-key]
  (let [fsm-file (fsm-file fsm-key)]
    (when (.exists fsm-file)
      (atom (apply dialogs/sm-core (read-string (slurp fsm-file)))))))

(defn run-fsm![fsm-id fsm fsm-event]
  (println ":: accepted2 >>" (pr-str fsm-id) "-for->" (pr-str fsm-event))
  (dialogs/run-fsm! fsm fsm-event)
  (save-fsm fsm-id fsm)
  (println ":: after:" (pr-str @fsm) " -with- " (pr-str (-> @fsm :value last)))
  (-> @fsm :value last :c-actions))

(defn create-fsm! [cache fsm-key] (cache/cache-fsm! cache fsm-key (dialogs/mk-sm-core)))

(def find-fsm! (partial cache/through!* load-fsm)) ;;takes [cache fsm-key]

;;
;;
;;
(defn restore[]
  (println ":: replaying legacy history:")
  (persist/replay-legacy-1)
  (println ":: replaying history:")
  (persist/replay-2)
  (println ":: starting with config:" config/config))
