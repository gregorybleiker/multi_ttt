(ns multittt.honoserver
  (:require ["npm:react"]
            ["npm:react-dom/server"]
            ["jsr:@hono/hono" :as hono]
            ["npm:@hono/node-server" :refer [serve]]
            ["npm:@starfederation/datastar-sdk/web" :as d]
            [reagent.dom.server :refer [render-to-string]]
            [promesa.core :as p]
            [applied-science.js-interop :as j]
            [multittt.state :as state]
            [multittt.stream :as stream]
            [multittt.game :as game]
            [multittt.frontend :as frontend]))

(defn get-signal [signals name]
  (j/get-in signals [:signals name]))

(defn route2! [r]
  (.get r "/" (fn [c] (.html c frontend/homepage)))
  (.get r "actions/redirect" (fn [c] (let [url (.query c.req "url")
                                           redirect_command (str "setTimeout(() => window.location = '" url "')")]
                                       (.stream d/ServerSentEventGenerator
                                                (fn [stream] (.executeScript stream redirect_command)
                                                  #js{:keepalive true})))))
  (.get r "actions/connect" (fn [c] (let [game-id (.get c "game-id")
                                          _ (println game-id)] (.stream d/ServerSentEventGenerator
                                                                        (partial state/stream-handler game-id) ;;game-id playertype frontend/status-message frontend/board-message)
                                                                        #js{:keepalive true}))))
  (.get r "/game" (fn [c] (let [gameid (.query c.req "game_id")] (.html c (frontend/gamepage @state/all-streams gameid))))))

(defn routes [req]
  (p/let [url (new js/URL req.url)
          path url.pathname
          params url.searchParams
          signals (.readSignals d/ServerSentEventGenerator req)
          game-id (get-signal signals "game_id")
          playertype (get-signal signals "playertype")]
    (case path
      "/game"
      (let [url-game-id (.get params "game_id")]
        (new js/Response  #js{:headers #js{:content-type "text/html"}}))
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
               (partial state/stream-handler game-id playertype frontend/status-message frontend/board-message)
               #js{:keepalive true})
      "/actions/redirect"
      (let [url_url (.get params "url")
            redirect_command (str "setTimeout(() => window.location = '" url_url "')")]
        (.stream d/ServerSentEventGenerator
                 (fn [stream] (.executeScript stream redirect_command)
                   #js{:keepalive true})))
      (new js/Response "nope"))))

;; Server
(defonce webserver (atom {}))
(defonce webrouter (atom {}))

(defn start [port]
  (reset! webrouter (hono/Hono.))
  (.use @webrouter
        (fn [c next]
          (let [signals (.readSignals d/ServerSentEventGenerator c.req)
                game-id (get-signal signals "game_id")
                playertype (get-signal signals "playertype")
                _ (println (str signals " -- " game-id))]
            (.set c "game-id" game-id)
            (.set c "playertype" playertype)
            (.then (next)))))
                 ;(fn []
                 ;  (js/console.log "middleware 1 end")))))
  (route2! @webrouter)
  (reset! webserver (serve #js {:fetch (.-fetch @webrouter) :port port})))

(defn stop []
  (.close @webserver))

(defn add-route []
  (.get @webrouter "/bla" (fn [c] (.text c "new route"))))
