(ns dialogs.core-messages
  (:require
   [clojure.java.io :as io]
   [clojure.string :as s]
   db
   lang
   evaluator
   persist)
  (:use dialogs.common))

;;
;;
;;
(def fsm-name-version :core-0.9.0)

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
      {:c-text (lang/format-lookup t l)}
      {:c-disposition :c-not-found
       :c-text (lang/format-not-found t)})))

(defmethod eval-parsed-event! :research [parsed-event]
  (let [t (-> parsed-event :args :topic)]
    (if (re-find #"(?i)luck" t)
      {:c-text (str ".\nYour lucky numbers are:" (s/join " " (take 6 (repeatedly #(+ 10 (rand-int 90)))))  "!")}
      {:c-text ".\nHi, I will ask around for that information and connect you with experts in a separate DM, please :whole-day:"
       :c-create-fsm-for (-> parsed-event :args)
       }
      )
    )) ;;TODO! add name, personalize

(defmethod eval-parsed-event! :help [parsed-event]
  {:c-text lang/format-help})

(defmethod eval-parsed-event! :evaluate [parsed-event]
  {:c-text (evaluator/evaluate (-> parsed-event :args :expression))})

(defmethod eval-parsed-event! :define [parsed-event]
  (persist/store parsed-event)
  (db/teach (-> parsed-event :args :term) (:definition (-> parsed-event :args) parsed-event))
  {:c-disposition :c-new-definition
   :c-text (lang/format-thx (-> parsed-event :args :term))})

(defmethod eval-parsed-event! :none [parsed-event]
  {:c-text "_it seems that you changed your mind or made a typo so this is not clover request any more, this will disappear when you delete your original request_"})


;;
;;
;;

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
           fsm-name-version
           (assoc parsed-event :c-evaled-event (eval-parsed-event! parsed-event) :c-fsm-id fsm-id)]))))

(defmethod process-slack-message! ["message" "message_sent"] [fsm-id rtm-event]
  [false
   fsm-name-version
   (assoc {:ack :ok} :rtm-event rtm-event :c-fsm-id fsm-id)])

(defmethod process-slack-message! ["message" "message_changed"] [fsm-id rtm-event]
  (let [{message :message} rtm-event]
    (when-let [parsed (lang/parse (:text message))]
      (let [parsed-event (assoc parsed :rtm-event rtm-event)]
        [(not= :none (:command parsed))
         fsm-name-version
         (assoc parsed-event :c-evaled-event (eval-parsed-event! parsed-event) :c-fsm-id fsm-id)]))))

(defmethod process-slack-message! ["message" "message_deleted"] [fsm-id rtm-event]
  [false
   fsm-name-version
   {:command :delete :rtm-event rtm-event :c-fsm-id fsm-id}])

(defmethod process-slack-message! :default [fsm-id rtm-event] nil)
