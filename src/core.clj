(ns core
  (:require
   [clojure.core.async :as async :refer [>! >!! <! <!! go go-loop]]
   [perseverance.core :as p]
   slack-rtm
   dialogs.core
   dialogs.research
   fsm
   config
   util
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

(defn ex-wrapper[e]
  (prn "ex wrapper" e)
  e)

(defn make-comm* [& args] (p/retry {} (p/retriable {:catch [Exception] :ex-wrapper ex-wrapper} (apply make-comm args))))

(defn send-message [c t]
  (Thread/sleep 2000)
  (>!! (second @comms) {:c-dispatch :c-post :c-channel c :c-text t} ))

(defn broadcast [t]
  (doall (map #(send-message % t) (slack-rtm/member-of (:api-token config/config)))))

(defn process-find-and-run[fsmf slack-unique-id process-slack-message! dialog-cache rtm-event]
  (let [fsm-id (slack-unique-id rtm-event)]
    (prn "AAAAA" fsm-id)
    (when-let [[create? fsm-name fsm-event] (process-slack-message! fsm-id rtm-event)]
      (prn "BBBBB" create? fsm-name fsm-event)
      (let [fsm (fsm/find-fsm! dialog-cache fsmf fsm-id)
            _ (prn "===RUN===" fsm-id fsm-name fsm-event fsm)]
        (when-let [fsm (if fsm
                         fsm
                         (if create?
                           (fsm/create-fsm! dialog-cache fsmf fsm-id)))]
          [fsm fsm-name fsm-id fsm-event])))))

(def fsms [(apply partial process-find-and-run dialogs.core/definition)
           (apply partial process-find-and-run dialogs.research/definition)
           (apply partial process-find-and-run dialogs.research/definition2)])

(defn -main [& args]
  (fsm/restore)
  (let [dialog-cache (cache/mk-fsm-cache)
        main-loop (go-loop [[in out stop] (make-comm)]
                    (if-let [rtm-event (<! in)]
                      (do
                        (try ;;->this is not atomic
                          (when-let [[fsm fsm-name fsm-id fsm-event] (some #(% dialog-cache rtm-event) fsms)]
                            (println ":: accepted1 >>" (pr-str fsm-name rtm-event))
                            ;;TODO! add protection against resending same action in case FSM did not accept event
                            (doseq [msg (fsm/run-fsm! fsm-name fsm-id fsm fsm-event)]
                              (>! out msg)))
                          (catch Exception e
                            (println "ERROR0:" e rtm-event)));;<-this is not atomic ;;TODO! add str repr                        
                        (recur [in out stop]))
                      (do ;; something wrong happened, re init ## that needs some love
                        (println ":: WARNING! The comms went down, going to restart.")
                        (stop)
                        (<! (async/timeout 3000))
                        (recur (make-comm*)))
                      ))]
    (<!! main-loop)
    (shutdown-agents)
    )
  )
