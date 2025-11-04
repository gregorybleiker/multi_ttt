(ns multittt.server
  (:require ["npm:react"]
            ["npm:react-dom/server"]
            ["npm:express$default" :as express]
            ["npm:@starfederation/datastar-sdk/web" :as d]
            [reagent.dom.server :refer [render-to-string]]
            [promesa.core :as p]
            [applied-science.js-interop :as j]
            [multittt.state :as state]
            [multittt.stream :as stream]
            [multittt.game :as game]
            [multittt.frontend :as frontend]))

;; Connection management
;

(defn stream-handler [game-id playertype stream]
  (state/ensure-init-board! game-id)
  (state/clean-stream! game-id frontend/status-message playertype)
  (state/add-stream! game-id playertype stream)
  (stream/broadcast @state/all-streams frontend/status-message frontend/board-message game-id))

(defn get-signal [signals name]
  (j/get-in signals [:signals name]))

(defn routes [req]
  (p/let [url (new js/URL req.url)
          path url.pathname
          params url.searchParams
          signals (.readSignals d/ServerSentEventGenerator req)
          game-id (get-signal signals "game_id")
          playertype (get-signal signals "playertype")]
    (case path
      "/"
      (new js/Response (render-to-string [:html frontend/head-part frontend/welcome-page]) #js{:headers #js{:content-type "text/html"}})
      "/game"
      (let [url-game-id (.get params "game_id")]
        (new js/Response (render-to-string [:html frontend/head-part (frontend/game-page @state/all-streams url-game-id)]) #js{:headers #js{:content-type "text/html"}}))
      "/actions/toggle"
      (let [current-player (get-in @state/all-streams [game-id :player])
            url_cell_id (parse-long (or (.get params "cell_id") ""))]
        (when (= playertype current-player)
          (state/update-board! game-id url_cell_id playertype)
          (let [board (get-in @state/all-streams [game-id :board])
                winner (game/check-win board)]
            (if winner
              (state/end-game! game-id frontend/status-message frontend/game-end-message frontend/end-button winner)
              (do
                (state/toggle-player! game-id)
                (stream/broadcast @state/all-streams frontend/status-message frontend/board-message game-id)))))
        (new js/Response))
      "/actions/connect"
      (.stream d/ServerSentEventGenerator
               (partial stream-handler game-id playertype)
               #js{:keepalive true})
      "/actions/redirect"
      (let [url_url (.get params "url")
            redirect_command (str "setTimeout(() => window.location = '" url_url "')")]
        (.stream d/ServerSentEventGenerator
                 (fn [stream] (.executeScript stream redirect_command)
                   #js{:keepalive true})))
      (new js/Response "nope"))))

;; Server
(defonce the-server nil)
(defonce e-server nil)

(defn start-server []
  (set! the-server (js/Deno.serve routes))
  (set! e-server (express.))
  (let [r (.Router express)]
  (.get r"/" (fn [req res]
                   (.send res "Birds home page")))
  (.use e-server r)
  (.listen e-server 9999))
  )

;; these are for the repl
(defn stop-server [] (.shutdown the-server))

(defn restart []
  (p/do!
   (reset! state/all-streams (hash-map))
   (stop-server)
   (start-server)
   ;; important: last expr should not be a promise, so fn returns only after all promises above are resolved
   (prn "restarted")))

(defn -main [] (p/do! (start-server)))
