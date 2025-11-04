(ns multittt.control
  (:require [multittt.server :as server]
            [multittt.state :as state]
            [promesa.core :as p]))

(defn start-server []
  (server/start)
  ; (set! e-server (express.))
  ; (let [r (.Router express)]
  ; (.get r"/" (fn [req res]
  ;                  (.send res "Birds home page")))
  ; (.use e-server r)
  ; (.listen e-server 9999))
  )

;; these are for the repl
(defn stop-server [] (server/stop))

(defn restart []
  (p/do!
   (state/clear-streams!)
   (server/stop)
   (server/start)
   ;; important: last expr should not be a promise, so fn returns only after all promises above are resolved
   (prn "restarted")))

(defn -main [] (p/do! (server/start)))
