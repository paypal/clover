(ns dialogs.research-messages
  (:require
   [clojure.java.io :as io])
  (:use dialogs.common)
  )

;;
;;
;;
(def fsm-name-version :research-0.9.0)

;;
;;
;;
(defmulti slack-unique-id slack-unique-id-selector)

(defmethod slack-unique-id ["message" nil] [rtm-event]
  (clojure.set/rename-keys (select-keys rtm-event [:thread_ts :user :team]) {:thread_ts :ts}))

(defmethod slack-unique-id ["create-fsm" "fsm-research"] [rtm-event]
  (-> rtm-event :c-self-fsm-id))

(defmethod slack-unique-id :default [rtm-event] nil)

;;
;;
;;
(defmulti slack-unique-id2 slack-unique-id-selector)

(defmethod slack-unique-id2 ["message" nil] [rtm-event]
  (clojure.set/rename-keys (select-keys rtm-event [:thread_ts :channel :team]) {:thread_ts :ts :channel :user}))

(defmethod slack-unique-id2 ["create-fsm" "fsm-research"] [rtm-event]
  (-> rtm-event :c-self-fsm-id))

(defmethod slack-unique-id2 :default [rtm-event] nil)

;;
;;
;;
(defmulti process-slack-message! (fn[fsm-id rtm-event] (slack-unique-id-selector rtm-event)))

(defmethod process-slack-message! ["message" nil] [fsm-id rtm-event]
  (prn "process-slack-message! " rtm-event)
  (let [t (-> :text rtm-event)]
    (when-let [resp (condp = (first t)
                      \1 :yes ;;TODO it reacts to mistake `1 @user` fix it
                      ;;\2 :no
                      \3 (second (re-find #"<@([A-Z0-9]+)>" t))
                      nil)]
      [false
       fsm-name-version
       {:resp resp :rtm-event rtm-event :c-fsm-id fsm-id}])));;TODO add messages which do not match this condp e.g. `3 @alw`

(defmethod process-slack-message! ["create-fsm" "fsm-research"] [fsm-id rtm-event]
  [true
   fsm-name-version
   {:rtm-event rtm-event :c-fsm-id fsm-id}])


(defmethod process-slack-message! :default [fsm-id rtm-event] nil)
