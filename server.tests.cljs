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

(deftest add-remove-test
  (testing "adding and removing leaves empty"
    (let [val #js{:a "b"}
          key 1]
      (is (empty?
           (:key (server/remove-stream
                  (server/add-stream (hash-map) key val)
                  key val)))))))

;; print name of each test
(defmethod t/report [:cljs.test/default :begin-test-var] [m]
  (println "===" (-> m :var meta :name))
  (println))

;; run this function with: nbb -m example/run-tests
(defn run-tests []
  (t/run-tests 'server.tests))
