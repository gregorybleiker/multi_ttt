(ns multittt.game)

(defn all-same [arr]
  (when (apply = arr) (first arr)))

(defn check-win [b]
;; combinations are the subsets of the board which are 3 in a row for tic tac toe
  (let [combinations (into [] (map #(map b %) ['(0 1 2) '(3 4 5) '(6 7 8) '(0 3 6) '(1 4 7) '(2 5 8) '(0 4 8) '(2 4 6)]))]
    (first (keep identity (map all-same combinations)))))

