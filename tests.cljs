(ns tests
  (:require
   [server :as server]
   [cljs.test :as t :refer [deftest is testing]]
   ))

(deftest check-win-test
  (testing "line check"
    (let [lineoneX (server/check-win ["X" "X" "X" nil nil nil nil nil nil])
          linetwoO (server/check-win [nil nil nil "O" "O" "O" nil nil nil])
          diagonal (server/check-win ["X" nil nil nil "X" nil nil nil "X"])
          ]
      (is (= lineoneX "X"))
      (is (= linetwoO "O"))
      (is (= diagonal "X")))))

;; print name of each test
(defmethod t/report [:cljs.test/default :begin-test-var] [m]
  (println "===" (-> m :var meta :name))
  (println))

;; run this function with: nbb -m example/run-tests
(defn run-tests []
  (t/run-tests 'tests))
