(ns expander
  (:require [clojure.string :as s])
  (:use dict))



;;## move to separate module

(defn lookup[t]
  (when-let [r (dic (last t))]
    (s/join ";" r)))
