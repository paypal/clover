(ns core
  (:require
   [clojure.core.async :as async :refer [>! >!! <! <!! go go-loop]]
   [perseverance.core :as p]
   state
   config
   util
   slack-rtm
   cache
   c)
  (:import java.lang.Thread)
  (:gen-class))

(def comms (atom []))

(defn make-comm []
  (let [id (:comm config/config)
        f (util/kw->fn id)
        _ (c/intln ":: building com:1" (:comm config/config))
        fr (f config/config)
        _ (c/intln ":: building com:2" (:comm config/config))
        ]
    (reset! comms fr)
    fr))

(defn ex-wrapper[e]
  (prn "ex wrapper" e)
  e)

(defn make-comm* [& args] (p/retry {} (p/retriable {:catch [Exception] :ex-wrapper ex-wrapper} (apply make-comm args))))

(defn send-message [c t]
  (Thread/sleep 2000)
  (>!! (second @comms) {:c-dispatch :c-post :c-channel c :c-text t} ))

(defn broadcast [t]
  (doall (map #(send-message % t) (slack-rtm/member-of (:api-token config/config)))))

(defn clover[]
  (state/restore)
  (let [dialog-cache (cache/mk-fsm-cache)
        main-loop (go-loop [[in out stop] (make-comm)]
                    (c/intln "IN GOOOOOO BLOCK" in out stop)
                    (if-let [rtm-event (<! in)]
                      (do
                        (try
                          (let [fsm-id (state/slack-unique-id rtm-event)]
                            (when-let [[create? fsm-event] (state/process-slack-message! fsm-id rtm-event)]
                              (let [fsm (state/find-fsm! dialog-cache fsm-id)]
                                (when-let [fsm (if fsm
                                                 fsm
                                                 (if create?
                                                   (state/create-fsm! dialog-cache fsm-id)))]
                                  ;;->this is not atomic
                                  (c/intln ":: accepted1 >>" (pr-str rtm-event))
                                  (doseq [msg (state/run-fsm! fsm-id fsm fsm-event)]
                                    (>! out msg))
                                  ;;<-this is not atomic
                                  ))))
                          (catch Exception e
                            (c/intln "ERROR0:" e rtm-event)));;TODO! add str repr
                        (recur [in out stop])
                        );;if-let do
                      (do ;; something wrong happened, re init ## that needs some love
                        (c/intln ":: WARNING! The comms went down, going to restart.:1");;##TODO really check this
                        (stop)
                        (c/intln ":: WARNING! The comms went down, going to restart.:2");;##TODO really check this
                        (<! (async/timeout 3000))
                        (c/intln ":: WARNING! The comms went down, going to restart.:3");;##TODO really check this
                        (let [xx (make-comm*)]
                          (c/intln "xxxxxxxxxx" xx)
                          (recur xx))
                        );;if-let do else
                      ))]
    main-loop))

(defn -main [& args]
  (c/intln "ALLLLLLLMOST")
  (<!! (clover))
  (c/intln "DOOOOOOOOONE")
  (shutdown-agents)
  (c/intln "DOOOOOOOOONE FOR REAL")
)
