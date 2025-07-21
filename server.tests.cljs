(ns server.tests
  (:require
   [server :as server]
   [cljs.test :as t :refer [async deftest is testing]]
   [promesa.core :as p]))

(comment
  (deftest server-test
    (testing "connection is saved"
      (server/streamhandler :s)
      (is (= (count @server/all-streams) 1)))))

(deftest check-win-test
  (testing "line check"
    (let [lineoneX (server/check-win ["X" "X" "X" " " " " " " " " " " " "])
          linetwoO (server/check-win [" " " " " " "O" "O" "O" " " " " " "])]
      (is (= lineoneX "X"))
      (is (= linetwoO "O")))))

;; print name of each test
(defmethod t/report [:cljs.test/default :begin-test-var] [m]
  (println "===" (-> m :var meta :name))
  (println))

;; run this function with: nbb -m example/run-tests
(defn run-tests []
  (t/run-tests 'server.tests))
