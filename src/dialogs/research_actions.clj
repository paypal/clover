(ns dialogs.research-actions
  (:require
   [reduce-fsm :as fsm]
   slack-rtm
   config)
  (:use dialogs.common))


(defn- action-new [args acc fsm-event from-state to-state]
  (prn "=========================NEW" args acc fsm-event from-state to-state)
  (conj acc {:c-event fsm-event :c-actions []}))

(defn- action-yes [acc fsm-event from-state to-state]
  (prn "=========================YES"  acc fsm-event from-state to-state)
  (let [trig-event (->> acc (filter #(-> % :c-event :rtm-event :c-args)) last)
        topic (-> trig-event :c-event :rtm-event :c-args :topic)
        c-help-seeker (-> trig-event :c-event :rtm-event :c-help-chain first)
        helping-user (-> fsm-event :rtm-event :user)
        resp1 {:c-dispatch :c-post
               :c-team (-> fsm-event :rtm-event :team)
               :c-channel (-> fsm-event :rtm-event :channel)
               :c-thread_ts (-> fsm-event :rtm-event :thread_ts)
               :c-text "This is awesome, PA will create a dedicated DM to further this converstation!"}
        dm (slack-rtm/conversation-open (:api-token config/config) [c-help-seeker helping-user])
        post-msg (slack-rtm/chat-postMessage (:api-token config/config) dm (format "Let's chat about _%s_" topic))
        #_resp2 #_{:c-dispatch :c-post :c-channel dm :c-text (str "Let's chat about _" topic "_")}]
    (conj acc {:c-event fsm-event :c-actions [resp1]}))
  )

;;TODO! handle  case when nobody wants to help
(defn- action-no [acc fsm-event from-state to-state]
  (prn "=X========================NO" acc fsm-event from-state to-state)
  (let [trig-event (->> acc (filter #(-> % :c-event :rtm-event :c-args)) last)
        resp1 {:c-dispatch :c-post
               :c-team (-> fsm-event :rtm-event :team)
               :c-channel (-> fsm-event :rtm-event :channel)
               :c-thread_ts (-> fsm-event :rtm-event :thread_ts)
               :c-text "Sorry for interrupting. We will try again in future."}]
    (conj acc {:c-event fsm-event :c-actions [resp1]}))
  )

(defn- action-already-answered [acc fsm-event from-state to-state]
  (prn "=X========================ALREADY" acc fsm-event from-state to-state)
  (let [trig-event (->> acc (filter #(-> % :c-event :rtm-event :c-args)) last)
        resp1 {:c-dispatch :c-post
               :c-team (-> fsm-event :rtm-event :team)
               :c-channel (-> fsm-event :rtm-event :channel)
               :c-thread_ts (-> fsm-event :rtm-event :thread_ts)
               :c-text "Action was already taken."}]
    (conj acc {:c-event fsm-event :c-actions [resp1]}))
  )

(defn- action-loop [user acc fsm-event from-state to-state]
  (prn "=X========================LOOP" user acc fsm-event from-state to-state)
  (let [resp1 {:c-dispatch :c-post
               :c-team (-> fsm-event :rtm-event :team)
               :c-channel (-> fsm-event :rtm-event :channel)
               :c-thread_ts (-> fsm-event :rtm-event :thread_ts)
               :c-text (format "<@%s> already participated in helping here. Please find somebody else or accept customer's request :-) !" user)}
        ]
    (conj acc {:c-event fsm-event :c-actions [resp1]}))
  )
(defn- action-user [user acc fsm-event from-state to-state]
  (prn "=X========================USER" user acc fsm-event from-state to-state)
  (let [trig-event (->> acc (filter #(-> % :c-event :rtm-event :c-args)) last)
        trig-rtm-event (-> trig-event :c-event :rtm-event)
        c-self-fsm-id (:c-self-fsm-id trig-rtm-event)
        args (:c-args trig-rtm-event)
        c-help-chain (:c-help-chain trig-rtm-event)
        resp1 {:c-dispatch :c-post
               :c-team (-> fsm-event :rtm-event :team)
               :c-channel (-> fsm-event :rtm-event :channel)
               :c-thread_ts (-> fsm-event :rtm-event :thread_ts)
               :c-text (format "Thank you! We will reach out to <@%s>." user)}
        resp2 (build-response-message c-self-fsm-id fsm-event args (:resp fsm-event) (conj c-help-chain (-> fsm-event :rtm-event :user)))]
    (conj acc {:c-event fsm-event :c-actions [resp1 resp2]}))
  )


(defn- loop? [[state event]]
  #_(< 2 (-> state first :c-event :rtm-event :c-help-chain count));;for local testing only
  (some #(= % (-> event :resp)) (-> state first :c-event :rtm-event :c-help-chain)))

(defn- show-transition [[state event]]
  (prn "STATE:" state)
  (prn "EVENT:" event)
  true)

;;
(fsm/defsm-inc fsm-research
  [
   [:ready {:is-terminal false}
    [[_ {:rtm-event {:c-args args}}]]   -> {:action (partial action-new args)} :asking
    ]
   [:asking {:is-terminal false}
    [[_ {:resp :yes}]]                -> {:action action-yes} :completed
    [[_ {:resp :no}]]                 -> {:action action-no} :completed
    [([_ {:resp user}] :guard loop?)] -> {:action (partial action-loop user)} :asking
    [ [_ {:resp user}]]               -> {:action (partial action-user user)} :completed
    ]
   [:completed {:is-terminal false};;TODO if it is true, SM can take :yes action twice for some reason -> reach out to reduce-fsm owner, maybe it is a bug
    [[_ {:resp x}]]                   -> {:action action-already-answered} :completed
    ]
   ]
  :dispatch :event-acc-vec ;; there is a lot of noise in the code cause by that switch, but we must dispatch both event and state
  )

