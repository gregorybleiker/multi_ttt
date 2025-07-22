(ns scratch)

(def a (atom (hash-map)))

(defn add-stream [state id stream]
  (update-in state [id :streams] (fn [v] (if v (conj v stream) #{stream})))
;  (update-in [id :board] (fn [b] (if b b [-1 -1 -1 -1 -1 -1 -1 -1 -1])))
  )


(swap! a add-stream "b" "uu")
@a
(count (get-in @a ["b" :streams]))
(comment
  @a
  (get-in @a ["b" :board])
  )

(swap! a assoc :v "hello")
(swap! a assoc :w "hello 2")


(into {} (filter (fn [b] (let [_ (prn (map? b))] (= (str (first b)) ":v"))) @a))


(def b (assoc {} :a "a"))

(for [[k v] @a] (prn k v))

(update-in @a [:v] (fn [old] (str old " bye")))

#js[1,2,3]

(j/array [1 2 3])

(require '[applied-science.js-interop :as j])

(js/Array.from [1 2])

(into-array [1 2])

(into-array #js[1,2])

(j/lit #js[1,2])

(def d [1 2 3 4])
(update d 0 (fn[_] 3))
d

(def ar [1 2 3])


(defn all-same [arr]
  (when (apply = arr) (first arr)))

;  if (empty? arr) true (let [startitem (first arr)]
;                          (when (every? #(= % startitem) arr) startitem ))))
  (comment
    (all-same [1 1 2])
    (all-same ["a" "a" "a"])
    )

(defn get-n [arr start n inc]
  "gets `n` items from an array starting at `start` and skipping `inc` items"
  (map arr (range start (+ start (* n inc)) inc)))

(comment
  (let [testarray [0 1 2 3 4 5 6 7 8]]
    (get-n testarray 2 3 2
           ))

  )
