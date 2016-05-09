(ns db
  (:require
   [config :as co]
   [clojure.string :as s]
   [clojure.math.combinatorics :as com]
   [clojure.set :as se])
  (:use [clojure.algo.generic.functor :only [fmap]])
  )

(defn combine [s] [(first s) (s/join "," (remove empty? (rest s)))])

(defn read-flat-file[f] (->> f slurp s/split-lines (map #(s/split % #"\t" -1))))

(def default-dic (->> co/config :db java.io.File. file-seq (filter #(= 1 (count (.getName %)))) (map read-flat-file) (apply concat) (map combine) (group-by (comp s/lower-case first)) (fmap #(->> % (map last) distinct))))

(def dic (atom default-dic))

(defn teach [term definition]
  (swap! dic update (s/lower-case term) #(conj % definition))
  [true (str "thx for defining term _" term "_")])

(defn split-lowercase-etc[s] (->> (s/split s #"[^A-Za-z0-9]") (remove empty?) (map s/lower-case) set))
(defn redundant [s1 s2]
  (let [ss1 (split-lowercase-etc s1)
        ss2 (split-lowercase-etc s2)]
    (cond
      (se/superset? ss1 ss2) [s2]
      (se/superset? ss2 ss1) [s1])))

(defn remove-similar[s]
  (when s
    (let [ds (distinct s)
          red (->> (com/combinations ds 2) (mapcat (partial apply redundant)) distinct set)]
      (vec (se/difference (set s) red)))))

(defn lookup-raw [term]
  [false (@dic (s/lower-case term))])

(defn lookup [term]
  [false (remove-similar (@dic (s/lower-case term)))])

(comment
  (def y (->> @dic (map second) (filter #(< 1 (count %))) (map #(com/combinations % 2))))
  (def x (map #(vector (-> % lookup second) (-> % lookup-raw second)) (keys @dic)))
  (defn ss[[s1 s2]] (= (set s1) (set s2)))
  (pprint (remove ss x))
  )
