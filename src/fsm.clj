(ns fsm
  (:require
   [clojure.java.io :as io]
   [reduce-fsm :as fsm]
   config
   persist
   cache))

;;
;;
;;
(defn mk-sm[fsmf] (atom (fsmf [])))

;;
;;
;;
(def fsm-dir (str (:db config/config) "/dialogs/"))

(defn- fsm-file [fsm-id] (io/as-file (str fsm-dir (:ts fsm-id) "-" (:team fsm-id) "-" (:user fsm-id))))

(defn- save-fsm [fsm-name fsm-id fsm]
  (let [fsm-file (fsm-file fsm-id)
        content (with-out-str (pr [fsm-name (:state @fsm) (:value @fsm)]))]
    (spit fsm-file content)))

;;TODO - when loading - check if version is same or compatible
;;TODO - add conversion of old state machines
(defn- load-fsm [fsmf fsm-id]
  (let [fsm-file (fsm-file fsm-id)]
    (when (.exists fsm-file)
      (let [data (read-string (slurp fsm-file))
            fsm-name (first data)
            fsm-data (rest data)]
        (atom (apply fsmf fsm-data))))))

(defn run-fsm![fsm-name fsm-id fsm fsm-event]
  (println ":: accepted2 >>" (pr-str fsm-id) "-for->" (pr-str fsm-event))
  (swap! fsm fsm/fsm-event fsm-event)
  (save-fsm fsm-name fsm-id fsm)
  (println ":: after:" (pr-str @fsm) " -with- " (pr-str (-> @fsm :value last)))
  (-> @fsm :value last :c-actions))

(defn create-fsm! [cache fsmf fsm-id] (cache/cache-fsm! cache fsm-id (mk-sm fsmf)))

(defn find-fsm![cache fsmf fsm-id] (cache/through!* (partial load-fsm fsmf) cache fsm-id))

;;
;;
;;
(defn restore[]
  (println ":: replaying legacy history:")
  (persist/replay-legacy-1)
  (println ":: replaying history:")
  (persist/replay-2)
  (println ":: starting with config:" config/config))
