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
