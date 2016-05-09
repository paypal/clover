(ns core
  (:require [config :as co]
            [util :as util]
            [evaluator :as evaluator]
            [commands]
            [persist]
            [slack-rtm :as sr]
            [clojure.core.async :as async :refer [>! >!! <! go go-loop]])
  (:import java.lang.Thread)
  (:gen-class))

(def comms (atom []))

(defn make-comm []
  (let [id (:comm co/config)
        f (util/kw->fn id)
        _ (println ":: building com:" (:comm co/config))
        fr (f co/config)]
    (reset! comms fr)
    fr
    ))

(defn send-message [c t]
  (Thread/sleep 2000);;##tmp
  (>!! (second @comms) {:channel c :text t} ))

(defn broadcast [t]
  (doall (map #(send-message % t) (sr/member-of (:api-token co/config))))
  )

(defn -main [& args]
  (println ":: replying history")
  (persist/replay (comp commands/parse-and-execute :input))
  (println ":: starting with config:" co/config)

  (go-loop [[in out stop] (make-comm)]
    #_(println ":: waiting for input")
    (if-let [form (<! in)]
      (do
        ;;(prn ">>>>" form)
        (when-let [input (:input form)]
          (do
            (when-let [[logit? res] (commands/parse-and-execute input)]
              (when logit?
                (persist/log form))
              (println "::form>>" (pr-str form))
              (println "::=> " res)
              (flush)
              (>! out {:channel (get-in form [:meta :channel]) :text res})
              )))
        (recur [in out stop]))
      ;; something wrong happened, re init ## that needs some love
      (do
        (println ":: WARNING! The comms went down, going to restart.");;## really check this
        (stop)
        (<! (async/timeout 3000))
        (recur (make-comm)))));;## contribute to public slack repo

  (.join (Thread/currentThread)))

;;:channel  http://stackoverflow.com/questions/27476313/private-message-slack-user-via-rtm
;;
;; incoming: {:type message, :channel G0XXXXXXX, :user U0RXXXXXX, :text (+), :ts 1457133934.000002, :team T0XXXXXXX}
;; form >>  {:input (+), :meta {:type message, :channel G0XXXXXXX, :user U0XXXXXXX, :text (+), :ts 1457133934.000002, :team T0XXXXXXX}}


;;add logging and errors
;;use https://github.com/brunoV/throttler


;;handle :: incoming: {:ok false, :reply_to 27, :error {:code -1, :msg slow down, too many messages...}}
