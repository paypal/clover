(ns tree)

;;below CUT is a work around clojail inability to evaluate multiple s-exprs (or mine figuring out how to do that), so we split by #";;[C]UT"


;;CUT

(def nice-messages (atom (cycle [
                                 "Let's try again..."
                                 "Are you planning to make a forest?"
                                 "Try again with 7."
                                 "Oops, I meant 6."
                                 "Perhaps you should consider buying Tesla."
                                 ])))

;;CUT

(defn nice-message[] (first (swap! nice-messages next)))

;;CUT

(defn row[n i]
  (let [ii (inc i)
        background (repeat (- n ii) false)
        foreground (repeat (dec (* 2 ii)) true)]
  (concat background foreground background)))

;;CUT

(defn tree[n] (map (partial row n) (range n)))

;;CUT

(defn transpose[m] (apply map list m))

;;CUT

(def flip reverse) ;;just a better name for our context

;;CUT

(defn reflexion[m] (map reverse m))

;;CUT

(defn angle->dir[n] (quot (rem (+ 360 (rem n 360)) 360) 90))

;;CUT

(def dir->op
  {0 identity                     ;;  0deg turn, noop, leave it unchanged
   1 (comp transpose flip)        ;; 90deg turn, or (comp reflexion transpose)
   2 (comp flip reflexion)        ;;180deg turn, or (comp reflexion flip)
   3 (comp transpose reflexion)}) ;;270deg turn, or (comp flip transpose)

;;CUT

(def dir->emoji ;;str ":" needed to avoid a conflict with a hack
  {0 (str ":" "north_tree:")
   1 (str ":" "east_tree:")
   2 (str ":" "south_tree:")
   3 (str ":" "west_tree:")})

;;CUT

(defn print_tree[t]
  "If 'i' is not acceptable, pull a prank."
  (if (> (:size t) 6)
    (println (nice-message))
    (let [dir (-> t :angle angle->dir)
          emo (-> dir dir->emoji)
          emo->pixel {true emo false ":space_tree:" nil "\n"} ;;`nil` used to represent end of line
          op (-> dir dir->op)
          output (->> t                 ;;take tree of trees
                      :size             ;;extract it's size
                      tree              ;;build tree
                      op                ;;rotate it
                      (interpose [nil]) ;;make sure we know where the line ends
                      (apply concat)    ;;and make it a single "stream" out of it
                      (map emo->pixel)  ;;map fore/background to "emoji" pixels
                      (apply str))]     ;;finally build a string to be sent to @clover
      (print (str "```.\n" output)))))  ;;"```."is a signal to @clover to print it as it is

;;CUT

(defn north_tree[i]
  "If `i` is a thing created or modifed by `*_tree`, it prints it out.
   If `i` is an integer, it creates a standing up tree of size 'i'"
  (if (map? i)
    (print_tree i)
    {:angle 0 :size i}))

;;CUT

(defn east_tree[t]
  "It rotates the tree `t` by 90 degrees clockwise."
  (update t :angle (partial + 90)))

;;CUT

(defn south_tree[t]
  "It rotates the tree `t` upside-down."
  (update t :angle (partial + 180)))

;;CUT

(defn west_tree[t]
  "It rotates the tree `t` by 90 degrees counter-clockwise."
  (update t :angle (partial + -90)))

;;(-> 6 north_tree east_tree north_tree)
