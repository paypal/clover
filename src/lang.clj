(ns lang
  (:require [clojure.string :as s]
            [instaparse.core :as insta]))

;;TODO generate it of instaparse grammar

(def format-help
  ".
*following forms are supported:*
`!help` - for help
`!explain <term>` or `?<term>` - explain _term_
`!define <term> = <description>` or `?<term> = <description>` - define _term_ as _description_
`'<LISP expression>` - evaluate expression using Clojure dialect e.g. `'(+ 1 2)` or `'(filter odd? [1 2 3])`

*interacting with clover:*
 - deleting initial request results in hiding response, convenient in larger channels (all definitions stay, duplicates are removed)
 - editing inital request is equal to deletion and posting new one while previous response is replaced

 - all new and missing definitions will receive human attention at #clover-mod (feel free to join and help)
")

(defn format-mod-not-found[user-name user-real-name args] (str "User:" " " user-real-name " " "(`" user-name "`)" " " "cannot find definition for term: " "_" (:term args) "_. Please advise user directly (it will be automated soon)."))

(defn format-mod-new-definition[user-name user-real-name args] (str "User:" " " user-real-name " " "(`" user-name "`)" " " "defined term: " "_" (:term args) "_ as:" "`" (:definition args) "`. This is for information only. In future there will be option to vet the definition." ))

(defn format-thx[term] (str "Thx for defining term _" term "_ (full editing/deletion is not supported yet, changes always results in new definition, type `!help` for more info.)"))

(defn format-not-found[term] (str "term _" term "_ is not registered (search is case insensitive but the database preserves it), @clover will try to find the definition for you or type `!help` for more options."))

(defn format-lookup[t d]
  (if (next d)
    (s/join "\n" (cons "*multiple definitions exist:*" (map-indexed #(str (inc %1) " - " %2) d)))
    (first d)))

(def read-clover-lang (insta/parser
    "clover-sentence = eval / define / explain / help / noop
     help = '!help'
     define = ('?' | '!define') break term break '=' break #'.+'
     explain = ('?' | '!explain') break term break
     term = word space term | word
     space = #'\\s+'
     word = #'[a-zA-Z0-9&-/]+'
     eval = #'[\\'\u2018]' catchall
     noop = catchall
     break = #'\\s*'
     catchall = #'([\\s\\S])*'"))

;;TODO add (= "?WF*$" s) [false "Working from Starbucks."] -> reqs

(def eval-stage
  (partial insta/transform {
                            :clover-sentence identity
                            :help (fn [_] {:command :help :args {}})
                            :word identity
                            :space (fn [_] " ")
                            :term str
                            :define (fn [_ _ term _ _ _ definition] {:command :define :args {:term term :definition definition}})
                            :explain (fn [_ _ term _] {:command :explain :args {:term term}})
                            :catchall identity
                            :eval (fn [_ expression] {:command :evaluate :args {:expression expression}})
                            :noop (fn [_] {:command :none :args nil})
                            }))

(defn parse [s]
  (when s
    (let [tree (read-clover-lang s)]
      (if (insta/failure? tree)
        (do
          (println ">>> Insta parse failure: " (pr-str tree))
          nil)
        (eval-stage tree)))))
