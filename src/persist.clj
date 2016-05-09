(ns persist
  (:require
   [config :as co]
   [clojure.string :as s]
   [clojure.edn :as e]
   commands))

(def log-file (str (:db co/config) "/log"))

(defn ts[] (.getTime (java.util.Date.)))

(defn log[e]
  (spit log-file (with-out-str (prn {:ts (ts) :entry e})) :append true))

(defn- replaying [f e]
  (println "replaying:" e)
  (f (:entry e)))

(defn replay[f]
  (let [e (->> log-file slurp s/split-lines (map e/read-string) (remove nil?))]
    (doall (map (partial replaying f) e))))
