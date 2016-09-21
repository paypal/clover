(ns lang-test
    (:require [lang :as cmd])
    (:use clojure.test))

;; Util function working with tree
(defn t-type [tree] (get tree 0))
(defn t-frst-value [tree] (get tree 1))
(defn t-values [tree] (rest tree))


(defn t-clover-sentence-type [tree]
    (->> tree
         t-frst-value
         t-type))

(defn t-clover-sentence-values [tree]
    (->> tree
         t-frst-value
         t-values))

(defn is-clover-type[sentence tree]
    (is (= sentence (t-clover-sentence-type tree))))

;; Test if a explain or define tree has the given term
;; Hopfully the term is always at the same place in a
;; parsed tree
(defn is-clover-term[term tree]
    (is (= term (->> tree
                     t-clover-sentence-values
                     ;; we travel along the tree to found
                     ;; the prased value.
                     next
                     second
                     second
                     second))))

(defn is-clover-definition [definition tree]
    (is (= definition (->> tree
                           t-clover-sentence-values
                           ((fn [v] (nth v 6))))))) ; flip arg would be nicer.


;;May be a test utility macro.
(defmacro defaretest
    [values & body]
    (cons 'do (map (fn [bindings]
                       `(deftest ~(last bindings)
                            (let ~(first bindings)
                                ;; Report correctly when values is not odd.
                                ~@body))) (partition 2 values))))


;; Test data
(def noop-t (cmd/read-clover-lang "random"))
(def explain1-t (cmd/read-clover-lang "!explain me"))
(def explain2-t (cmd/read-clover-lang "?what"))
(def define1-t (cmd/read-clover-lang "!define me = whom are you?"))
(def define2-t (cmd/read-clover-lang "?me = You know me!!"))

;; Simple assertion on parse expression
(defaretest ([tree noop-t] common-read-noop
             [tree explain1-t] common-read-explain1
             [tree explain2-t] common-read-explain2
             [tree define1-t] common-read-define1
             [tree define2-t] common-read-define2)
    (is (= :clover-sentence (t-type noop-t)))
    (is (vector? (t-frst-value noop-t))))

(deftest read-noop-stage
    (is-clover-type :noop noop-t))

(defaretest ([tree explain1-t
              term "me"] read-explain1-stage
             [tree explain2-t
              term "what"] read-explain2-stage)
    (is-clover-type :explain tree)
    (is-clover-term term tree))

(defaretest ([tree define1-t
              term "me"
              definition "whom are you?"] read-define1-stage
             [tree define2-t
              term "me"
              definition "You know me!!"] read-defin2-stage)
    (is-clover-type :define tree)
    (is-clover-term term tree)
    (is-clover-definition definition tree))
