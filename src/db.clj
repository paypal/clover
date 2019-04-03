(ns db
  (:require
   [clojure.string :as s]
   [clojure.math.combinatorics :as com]
   [clojure.set :as se]
   config)
  (:use [clojure.algo.generic.functor :only [fmap]])
  )

(defn combine [s] [(first s) (s/join "," (remove empty? (rest s)))])

(defn read-flat-file[f]
  (->> f
       slurp
       s/split-lines
       (map #(s/split % #"\t" -1))))

;;TODO move to persist
(def default-dic
  (->> config/config
       :db
       java.io.File.
       file-seq(filter #(= 1 (count (.getName %))))
       (map read-flat-file)
       (apply concat)
       (map combine)
       (group-by (comp s/lower-case first))
       (fmap #(->> % (map last) distinct))))

(def dic (atom {}))





;;TODO Clover timer in responses
;;TODO Clover move token to protected maybe
;;TODO Clover - add version
;;TODO Clover might count down for error messages
;;TODO Clover - remove:message from clover-mod
;;TODO Clover autos about now known acronyms.
;;TODO Reword “_it seems that you changed your mind or made a typo so this is not clover request any more, this will disappear when you delete your original request_ (edited)” -> message perhaps
;;TODO Clover - make it disappear after a while "_it seems that you changed your mind or made a typo so this is not clover request any more, this will disappear when you delete your original request_ (edited)"
;;TODO CLOVER !explain -> ping user ->? Clover
;;TODO https://api.slack.com/docs/message-threading

;;{"term1" "def1" "term2" "def2"}
;;{"term1"  <#{metags}  #{defs}  #{authors fixit} >}


(defn teach [args]
  (let [{:keys [term definition]} args]
    (swap! dic update (s/lower-case term) #(conj % definition))))

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
          red (->> (com/combinations ds 2)
                   (mapcat (partial apply redundant))
                   distinct
                   set)]
      (vec (se/difference (set s) red)))))

(defn lookup-raw [term]
  (@dic (s/lower-case term)))

(defn lookup [term]
  (remove-similar (@dic (s/lower-case term))))

(comment
  (def y (->> @dic (map second) (filter #(< 1 (count %))) (map #(com/combinations % 2))))
  (def x (map #(vector (-> % lookup second) (-> % lookup-raw second)) (keys @dic)))
  (defn ss[[s1 s2]] (= (set s1) (set s2)))
  (pprint (remove ss x))
  )
