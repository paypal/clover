(ns dict
  (:require [clojure.string :as s])
  (:use [clojure.algo.generic.functor :only [fmap]]))

(defn combine [s] [(first s) (s/join "," (remove empty? (rest s)))])

(defn read-flat-file[f] (->> f slurp s/split-lines (map #(s/split % #"\t" -1))))

(def dic (->> ["db/1" "db/2" "db/3"] (map read-flat-file) (apply concat) (map combine) (group-by first) (fmap #(->> % (map last) distinct))))
