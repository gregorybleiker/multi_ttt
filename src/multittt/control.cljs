(ns multittt.control
  (:require [multittt.server :as server]
            [multittt.state :as state]
            [nbb.nrepl-server :as nrepl]
            [promesa.core :as p]))
            
(def port 8000)
(defn start-server []
  (server/start port)
  ;(server/add-route)
  )

;; these are for the repl
(defn stop-server [] (server/stop))

(defn restart []
  (p/do!
   (state/clear-streams!)
   (server/stop)
   (server/start port)
   ;; important: last expr should not be a promise, so fn returns only after all promises above are resolved
   (prn "restarted")))

(defn -main [] (p/do!
                (nrepl/start-server! {:port 1337})
                (start-server)))
