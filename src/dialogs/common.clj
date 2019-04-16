(ns dialogs.common
  (:require config
            lang
            slack-rtm
            persist))

(defn slack-unique-id-selector [rtm-event] (mapv rtm-event [:type :subtype]))

(def user-format-mod-m {:c-new-definition lang/format-mod-new-definition :c-not-found lang/format-mod-not-found})

;;TODO this is the place when an intial POC is determined, plug AI in future
(defn- find-an-expert[t]
  (cond
    (re-find #"(?i)patent" t) "U........"
    (re-find #"(?i)idea" t) "U........"
    (re-find #"(?i)andy" t) "U........"
    :else (:research-channel config/config)
    )
  )

(defn build-response-message[parent-fsm-id fsm-event args to from]
  (let [rtm-event (:rtm-event fsm-event)
        msg (str "A customer is looking for an expert in: _" (:topic args) "_. Please respond *in a thread* with:
`1` when you would like to help
~`2` hmm, may be not~ : _it is *not an option* at PayPal, is it? WIP though_
`3 @name` when you know somebody who might know the answer (name is slack user name)
In case you are confused with this post, please consult with #clover-dev.
")
        dm (if (.startsWith to "U") (slack-rtm/im-open (:api-token config/config) to) to) ;;TODO optimially we should use RTM
        post-msg (slack-rtm/chat-postMessage (:api-token config/config) dm msg)
        team  (first (filter identity [(:team post-msg) (:team rtm-event)])) ;;first non nil
        child-fsm-id {:ts (:ts post-msg) :team team :user to}
        rtm-event (:rtm-event fsm-event)
        ]
    {:c-dispatch :c-send-back
     :type "create-fsm"
     :subtype "fsm-research"
     :c-team (:team child-fsm-id)
     :c-user to
     :c-args args
     :c-self-fsm-id child-fsm-id
     :c-help-chain from
     :c-parent-fsm-id parent-fsm-id}
    ;;TODO (persist/log-response [fsm-id args rtm-event evaled-event full-resp]) for all messages posted outside RTM    
    ))

(defn build-response[args fsm-resp evaled-event fsm-id fsm-event] ;; TODO do I need both evaled-event and fsm-event here
  (let [rtm-event (:rtm-event fsm-event)
        full-resp (concat [fsm-resp]
                          (when (:c-disposition evaled-event)
                            [{:c-dispatch :c-post
                              :c-team (:team fsm-id);;TODO! fix fsm-id > evaled-event?
                              :c-channel (:mod-channel config/config)
                              :c-text (((:c-disposition evaled-event) user-format-mod-m) (:name rtm-event) (:real_name rtm-event) args)}])
                          (when (:c-create-fsm-for evaled-event)
                            [(build-response-message fsm-id fsm-event (:c-create-fsm-for evaled-event) (find-an-expert (-> evaled-event :c-create-fsm-for :topic)) [(-> rtm-event :user)])])
                          )]
    (persist/log-response [fsm-id args rtm-event evaled-event full-resp])
    full-resp))
