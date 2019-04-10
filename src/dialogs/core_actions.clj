(ns dialogs.core-actions
  (:require [reduce-fsm :as fsm])
  (:use dialogs.common))

(defn- find-initial-ts-channel[acc]
  (let [rtm-event (->> acc (map :c-event) (filter :ack) first :rtm-event)]
    [(-> rtm-event :c-payload :channel) (-> rtm-event :reply :ts)]))

(defn- action-new [command args evaled-event fsm-id acc fsm-event from-state to-state]
  (let [fsm-resp {:c-dispatch :c-ackpost :c-channel (-> fsm-event :rtm-event :channel) :c-text (:c-text evaled-event) :c-context fsm-event}
        actions (build-response args fsm-resp evaled-event fsm-id fsm-event)]
    (conj acc {:c-event fsm-event :c-actions actions})))

(defn- action-edit [command args evaled-event fsm-id acc fsm-event from-state to-state]
  (let [[channel ts] (find-initial-ts-channel acc)
        fsm-resp {:c-dispatch :c-update :c-ts ts :c-channel channel :c-text (:c-text evaled-event)}
        actions (build-response args fsm-resp evaled-event fsm-id fsm-event)]
    (conj acc {:c-event fsm-event :c-actions actions})))

(defn- action-delete [fsm-id acc fsm-event from-state to-state]
  (let [[channel ts] (find-initial-ts-channel acc)
        resp {:c-dispatch :c-delete :c-ts ts :c-channel channel}]
    (conj acc {:c-event fsm-event :c-actions [resp]})))

(defn- action-ignore [acc fsm-event from-state to-state]
  (conj acc {:c-event fsm-event :c-actions nil}))

;; trade-off requiring a lower level library vs. issues with decoupling macro from data
(fsm/defsm-inc fsm-core
  [
   [:ready {:is-terminal false}
    {:command command :args args :c-evaled-event evaled-event :c-fsm-id fsm-id} -> {:action (partial action-new command args evaled-event fsm-id)} :ack-wait
    ]
   [:ack-wait {:is-terminal false}
    {:ack :ok} -> {:action action-ignore} :responded
    ]
   [:responded {:is-terminal false};; this is only needed due to a bug in reduce-fsm
    {:command command :args args :c-evaled-event evaled-event :c-fsm-id fsm-id} -> {:action (partial action-edit command args evaled-event fsm-id)} :responded
    {:command :delete :c-fsm-id fsm-id} -> {:action (partial action-delete fsm-id)} :completed
    ]
   [:completed {:is-terminal true}
    ]
   ]
  )
