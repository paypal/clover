(ns db
  (:require [clojure.string :as s])
  (:use [clojure.algo.generic.functor :only [fmap]]))

(defn combine [s] [(first s) (s/join "," (remove empty? (rest s)))])

(defn read-flat-file[f] (->> f slurp s/split-lines (map #(s/split % #"\t" -1))))

(def default-dic (->> ["db/1" "db/2" "db/3" "db/4" "db/5"] (map read-flat-file) (apply concat) (map combine) (group-by (comp s/lower-case first)) (fmap #(->> % (map last) distinct))))

(def dic (atom default-dic))

(defn lookup [term]
  [false (@dic term)]
  )
(defn teach [term definition]
  (swap! dic update term #(conj % definition))
  [true (str "thx for defining term _" term "_")])
