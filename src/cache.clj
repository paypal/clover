(ns cache
  (:require
   [clojure.core.cache :as c]
   [clojure.java.io :as io]))

(defn mk-fsm-cache[]
  (-> {}
      (c/fifo-cache-factory :threshold 1024)
      (c/ttl-cache-factory  :ttl 60000)
      atom))

(defn cache-fsm! [cache fsm-key fsm]
  (swap! cache c/miss fsm-key fsm)
  fsm)

(defn through!* [value-fn cache item]
  (if (c/has? @cache item)
    (do
      (swap! cache c/hit item)
      (c/lookup @cache item))
    (when-let [v (value-fn item)]
      (swap! cache c/miss item v)
      v)))
