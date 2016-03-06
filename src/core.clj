(ns core
  (:require [config :as config]
            [util :as util]
            [evaluator :as evaluator]
            [commands :as commands]
            [clojure.core.async :as async :refer [>! <! go go-loop]])
  (:import java.lang.Thread)
  (:gen-class))

(defn make-comm [id config]
  (let [f (util/kw->fn id)]
    (f config)))

(defn -main [& args]
  (let [config (config/read-config)
        inst-comm (fn []
                    (println ":: building com:" (:comm config))
                    (make-comm (:comm config) config))]
    (println ":: starting with config:" config)

    (go-loop [[in out stop] (inst-comm)]
      #_(println ":: waiting for input")
      (if-let [form (<! in)]
        (do
          (when-let [input (:input form)]
            (when-let [res (commands/find input)]
              (println ":: form >> " form)
              (println ":: => " res)
              (>! out (assoc form :evaluator/result res :evaluator/channel "D0XXXXXXX"))))
          (recur [in out stop]))
        ;; something wrong happened, re init ## that needs some love
        (do
          (println ":: WARNING! The comms went down, going to restart.")
          (stop)
          (<! (async/timeout 3000))
          (inst-comm))))

    (.join (Thread/currentThread))))

;;:channel  http://stackoverflow.com/questions/27476313/private-message-slack-user-via-rtm
;;
;; incoming: {:type message, :channel G0XXXXXXX, :user U0RXXXXXX, :text (+), :ts 1457133934.000002, :team T0XXXXXXX}
;; form >>  {:input (+), :meta {:type message, :channel G0XXXXXXX, :user U0XXXXXXX, :text (+), :ts 1457133934.000002, :team T0XXXXXXX}}


;;add logging and errors
;;use https://github.com/brunoV/throttler


;;handle :: incoming: {:ok false, :reply_to 27, :error {:code -1, :msg slow down, too many messages...}}
