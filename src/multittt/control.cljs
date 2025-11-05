(ns multittt.control
  (:require [multittt.honoserver :as server]
            [multittt.state :as state]
            [promesa.core :as p]))

(defn start-server []
  (server/start)
  ;(server/add-route)
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

(defn -main [] (p/do! (start-server)))
