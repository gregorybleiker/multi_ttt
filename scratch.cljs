(ns scratch)

(def a (atom (hash-map)))

(swap! a assoc :v "hello")
(swap! a assoc :w "hello 2")


(into {} (filter (fn [b] (let [_ (prn (map? b))] (= (str (first b)) ":v"))) @a))


(def b (assoc {} :a "a"))

(for [[k v] @a] (prn k v))

(update-in @a [:v] (fn [old] (str old " bye")))
