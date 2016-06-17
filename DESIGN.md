## Throtlling

This is an extracted code showing the approach:

```
(require '[clojure.core.async :as async :refer [go-loop]])
(require '[throttler.core :refer [throttle-chan throttle-fn]])
(def in (async/chan 1024))(def slow-chan (throttle-chan in 40 :minute))
(async/go-loop [] (let [x (async/<! slow-chan)] (prn "diff" (- (System/currentTimeMillis) x)) (recur)))
(dotimes [_ 40] (>!! in (System/currentTimeMillis)))

```
