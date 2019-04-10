(ns dialogs.common
  (:require config
            lang
            persist))

(defn slack-unique-id-selector [rtm-event] (mapv rtm-event [:type :subtype]))

(def user-format-mod-m {:c-new-definition lang/format-mod-new-definition :c-not-found lang/format-mod-not-found})

(defn build-response[args fsm-resp evaled-event fsm-id fsm-event]
  (let [rtm-event (:rtm-event fsm-event)
        full-resp (concat [fsm-resp]
                          (when (:c-disposition evaled-event)
                            [{:c-dispatch :c-post
                              :c-team (:team fsm-id)
                              :c-channel (:mod-channel config/config)
                              :c-text (((:c-disposition evaled-event) user-format-mod-m) (:name rtm-event) (:real_name rtm-event) args)}]))]
    (persist/log-response [fsm-id args rtm-event evaled-event full-resp])
    full-resp))
