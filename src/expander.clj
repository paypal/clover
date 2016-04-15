(ns expander
  (:require [clojure.string :as s]
            [evaluator]
            db))

;;## move to separate module

(defn help[a]
  [false "*clover supports following forms:*
`!help` - for help
`!explain <term>` or `?<term>` - explain _term_
`!define <term> = <description>` or `?<term> = <description>` - define _term_ as _description_
"]
  )
(defn format-lookup[t d]
  (if d
    (if (next d)
      (s/join "\n" (cons "*multiple definitions exist:*" (map-indexed #(str (inc %1) " - " %2) d)))
      (first d))
    (str "term _" t "_ is not registered (search is case insensitive but the database preserves it), @clover will try to find the definition for you or type `!help` for more options.")))

(defn lookup[a]
  (let [t (last a)
        [u d] (db/lookup t)]
    [u (format-lookup t d)]))

(defn teach[a]
  (let [[u d] (db/teach (second a) (last a))]
    [u d]))

(defn evaluate[a] (-> a last evaluator/evaluate))
