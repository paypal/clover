(comment
  (require '[clojure.edn :as edn])
  (require '[clojure.java.io :as io])

  (def db ".../db/use")
  (with-open [in (-> db io/reader java.io.PushbackReader.)]
    (let[edn-seq (repeatedly (partial edn/read {:eof nil} in))]
      (def all (->> edn-seq (take-while (partial not= nil)) (remove empty?) doall))))


  (defn ttt [f] (quot (- 1571074255 (int (Float/parseFloat f))) (* 60 60 24)))

  (defn tt1[m] {:ts (ttt (:ts m)) :rn? (contains? m :real_name) :nm? (contains? m :name) :u? (clojure.string/starts-with? (:user m) "U")})
  (defn tt4[m] {:ts (ttt (:ts m)) :team (:team m) :rn? (contains? m :real_name) :nm? (contains? m :name) :u? (clojure.string/starts-with? (:user m) "U")})

  (->> all (map #(get-in % [:entry :rtm-event])) (filter #(-> % :subtype nil?)) (map tt1) (filter #(< (% :ts) 100)) (map #(dissoc % :ts)) frequencies (sort-by (comp :u? first)) pprint)

  (->> all (map #(get-in % [:entry :rtm-event])) (filter #(-> % :subtype nil?)) (map tt2) (map #(dissoc % :ts)) frequencies (sort-by (comp :u? first)) pprint)
  )
