(ns scratch)

(def a (atom (hash-map)))

(defn add-stream [state id stream]
  (-> state
  (update-in [id :streams] (fn [v] (if v (conj v stream) #{stream})))
  (update-in [id :board] (fn [b] (if b b [-1 -1 -1 -1 -1 -1 -1 -1 -1])))
  )
 )

(swap! a add-stream "b" "ab")
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
