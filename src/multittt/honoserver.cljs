(ns multittt.honoserver
  (:require ["npm:react"]
            ["npm:react-dom/server"]
            ["jsr:@hono/hono" :as hono]
            ["npm:@hono/node-server" :refer [serve]]
            ["npm:@starfederation/datastar-sdk/web" :as d]
            [promesa.core :as p]
            [applied-science.js-interop :as j]
            [multittt.state :as state]
            [multittt.stream :as stream]
            [multittt.game :as game]
            [multittt.frontend :as frontend]))

(defn get-signal [signals name]
  (if signals
    (j/get-in signals [:signals name])
    nil))

(defn route2! [r]
  (.get r "/" (fn [c] (.html c frontend/homepage)))
  (.get r "actions/redirect" (fn [c] (let [url (.query c.req "url")
                                           redirect_command (str "setTimeout(() => window.location = '" url "')")]
                                       (.stream d/ServerSentEventGenerator
                                                (fn [stream] (.executeScript stream redirect_command)
                                                  #js{:keepalive true})))))
  (.get r "actions/connect" (fn [c] (let [game-id (.get c "game-id")
                                          playertype (.get c "playertype")]
                                      (.stream d/ServerSentEventGenerator
                                               (partial state/stream-handler
                                                        game-id playertype
                                                        frontend/status-message
                                                        frontend/board-message)))))
  (.get r "actions/toggle" (fn [c]
                             (let [game-id (.get c "game-id")
                                   playertype (.get c "playertype")
                                   current-player (get-in @state/all-streams [game-id :player])
                                   url_cell_id (parse-long (or (.query c.req "cell_id") ""))]
                               (when (= playertype current-player)
                                 (state/update-board! game-id url_cell_id playertype)
                                 (let [board (get-in @state/all-streams [game-id :board])
                                       winner (game/check-win board)]
                                   (if winner
                                     (state/end-game! game-id frontend/status-message frontend/game-end-message frontend/end-button winner)
                                     (do
                                       (state/toggle-player! game-id)
                                       (stream/broadcast @state/all-streams frontend/status-message frontend/board-message game-id)))))
                               (new js/Response))))
  (.get r "/game" (fn [c] (let [game-id (.query c.req "game_id")] (.html c (frontend/gamepage @state/all-streams game-id)))))
  (.get r "*" (fn [c] (.text c "nope"))))

(defn routes [req]
  (p/let [url (new js/URL req.url)
          path url.pathname
          params url.searchParams
          signals (.readSignals d/ServerSentEventGenerator req)
          game-id (get-signal signals "game_id")
          playertype (get-signal signals "playertype")]
    (case path
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
      (new js/Response "nope"))))

;; Server
(defonce webserver (atom {}))
(defonce webrouter (atom {}))

(defn signalware [c next]
  (p/let [signals (.readSignals d/ServerSentEventGenerator c.req)]
    (when signals
      (let [_ (println (js/JSON.stringify c.req))
            game-id (get-signal signals "game_id")
            playertype (get-signal signals "playertype")]
        (.set c "game-id" game-id)
        (.set c "playertype" playertype)))
    (p/do! (next))))

(defn start [port]
  (reset! webrouter (hono/Hono.))
  (.use @webrouter signalware)
  (route2! @webrouter)
  (reset! webserver (serve #js {:fetch (.-fetch @webrouter) :port port})))

(defn stop []
  (.close @webserver))

(defn add-route []
  (.get @webrouter "/bla" (fn [c] (.text c "new route"))))
