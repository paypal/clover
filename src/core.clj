(ns core
  (:require
   [clojure.core.async :as async :refer [>! >!! <! go go-loop]]
   state
   config
   util
   slack-rtm
   cache)
  (:import java.lang.Thread)
  (:gen-class))

(def comms (atom []))

(defn make-comm []
  (let [id (:comm config/config)
        f (util/kw->fn id)
        _ (println ":: building com:" (:comm config/config))
        fr (f config/config)]
    (reset! comms fr)
    fr))

(defn send-message [c t]
  (Thread/sleep 2000)
  (>!! (second @comms) {:c-dispatch :c-post :c-channel c :c-text t} ))

(defn broadcast [t]
  (doall (map #(send-message % t) (slack-rtm/member-of (:api-token config/config)))))

(defn -main [& args]
  (state/restore)
  (let [dialog-cache (cache/mk-fsm-cache)]
    (go-loop [[in out stop] (make-comm)]
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
                    (println ":: accepted1 >>" (pr-str rtm-event))
                    (doseq [msg (state/run-fsm! fsm-id fsm fsm-event)]
                      (>! out msg))
                    ;;<-this is not atomic
                    ))))
            (catch Exception e
              (println "ERROR0:" e rtm-event)))
          (recur [in out stop])
          );;if-let do
        (do ;; something wrong happened, re init ## that needs some love
          (println ":: WARNING! The comms went down, going to restart.:")
          (stop)
          (<! (async/timeout 3000))
          (recur (make-comm))
          );;if-let do else
        )))
  (.join (Thread/currentThread)))
