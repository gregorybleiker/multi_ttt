(ns prom
  (:require [promesa.core :as p]))

(defn sleep [ms]
  (js/Promise.
   (fn [resolve _]
     (js/setTimeout resolve ms))))

(defn do-stuff
  [a]
  (p/do!
   (println (str "Doing stuff which takes a while " a))
   (sleep 1000)
   1))

;; (p/let [a (do-stuff)
;;         b (inc a)
;;         c (do-stuff)
;;         d (+ b c)]
;;   (prn d))

(defn doit3 []
  (p/do
   (do-stuff 1)
   (prn "one")
   (do-stuff 2)
   (prn "two"))
  "done")
(defn doit []
  (p/let [
    a (do-stuff 1)
    b (prn "one")
    c (do-stuff 3)
    d (prn "two")]
         "done"))

(p/do! (doit))
(prn "end")

(p/let [a (do-stuff 1)
        b (inc a)
        c (do-stuff 2)
        d (+ b c)]
  (prn d))
