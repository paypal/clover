(ns persist
  (:require
   [config :as co]
   [clojure.string :as s]
   [clojure.edn :as e]
   lang
   db
   c))

(def log-file-legacy-1 (str (:db co/config) "/log"))

(def log-file-2 (str (:db co/config) "/log2"))

(def use-file (str (:db co/config) "/use"))

(def response-file (str (:db co/config) "/response"))

(defn ts[] (.getTime (java.util.Date.)))

(defn- append-to[l e]
  (spit l (with-out-str (prn {:ts (ts) :entry e})) :append true))

(def store (partial append-to log-file-2))

(def log-use (partial append-to use-file))

(def log-response (partial append-to response-file))

(defn- replaying-legacy-1 [e]
  (c/intln "replaying legacy:" e)
  (let [{{:keys [term definition]} :args} (lang/parse (-> e :entry :input))]
    (when definition
      (db/teach term definition))))

(defn- replaying-2 [e]
  (c/intln "replaying:" e)
  (let [{:keys [term definition]} (-> e :entry :args)]
    (db/teach term definition)))

(defn- replay[l f]
  (let [e (->> l slurp s/split-lines (map e/read-string) (remove nil?))]
    (doall (map f e))))

(def replay-legacy-1 (partial replay log-file-legacy-1 replaying-legacy-1))

(def replay-2 (partial replay log-file-2 replaying-2))
