(ns multittt.server
  (:require ["npm:react"]
            ["npm:react-dom/server"]
            ["npm:express$default" :as express]
            [reagent.dom.server :refer [render-to-string]]
            ["npm:@starfederation/datastar-sdk/web" :as d]
            [promesa.core :as p]
            [applied-science.js-interop :as j]
            [multittt.state :as state]
            [multittt.stream :as stream]))

;; gameplay validation
(defn all-same [arr]
  (when (apply = arr) (first arr)))

(defn check-win [b]
;; combinations are the subsets of the board which are 3 in a row for tic tac toe
  (let [combinations (into [] (map #(map b %) ['(0 1 2) '(3 4 5) '(6 7 8) '(0 3 6) '(1 4 7) '(2 5 8) '(0 4 8) '(2 4 6)]))]
    (first (keep identity (map all-same combinations)))))

;; frontend related
(defn to-js [s] (js/JSON.stringify (clj->js s)))
(defn board-to-fragment [board winner]
  (into [:div {:class "grid" :id "board"}]
        (for [x (range 0 9)]
          (let [value (get board x)
                normalclass "cell is-flex is-align-items-center is-justify-content-center is-size-1"
                winnerclass (if (and (= winner "X") (= value "X")) "is-underlined"
                                (if (and (= winner "O") (= value "O")) "is-underlined" ""))
                classes (str normalclass " " winnerclass)]
            [:div {:class classes :style {:border "1px solid" :aspect-ratio "1"} :id (str "cell-" x) :data-on-click (str "@get('/actions/toggle?cell_id=" x "')")} value]))))

(defn status-message [text] (str "<div id='status'>" text "</div>"))
(defn board-message [board] (render-to-string (board-to-fragment board nil)))
(defn game-end-message [board winner] (render-to-string (board-to-fragment board winner)))
(def end-button (render-to-string [:button {:class "button" :data-on-click "@get('/actions/redirect?url='+encodeURI('/'))" :id "endedbutton"} "restart"]))

(def head-part
  [:head
   [:script {:type "module" :src "https://cdn.jsdelivr.net/gh/starfederation/datastar@main/bundles/datastar.js"}]
   [:link {:rel "stylesheet" :href  "https://cdn.jsdelivr.net/npm/bulma@1.0.4/css/bulma.min.css"}]])

(def welcome-page
  [:body
   [:div  {:data-signals "{game_id: ''}"}]
   [:section {:class "section"}
    [:div {:class "container has-text-centered"}
     [:h1 {:class "title"} "Start a Game"]
     [:div {:class "block"}
      [:input {:data-bind "game_id"}]]
     [:div {:class "block"}
      [:button {:class "button" :data-show "$game_id != ''"
                :data-on-click "@get( '/actions/redirect?url=' + encodeURI('/game?game_id=' + $game_id.toUpperCase()))"}
       [:span {:data-text "'Start Game ' + $game_id.toUpperCase()"}]]]]]])

(defn game-page [game-id]
  (let [playertype (if-not (get-in @state/all-streams [game-id :streams "X"]) "X"
                           (if-not (get-in @state/all-streams [game-id :streams "O"]) "O"
                                   "Full"))
        board (get-in @state/all-streams [game-id :board])]
    (if (= playertype "Full") [:body [:h1 "Sorry, we're full"]]
        [:body
         [:div  {:data-signals (str "{game_id:" (to-js game-id) ", playertype: " (to-js playertype) ", board:'[]'}")}]
         [:div {:data-on-load "@get('/actions/connect')"}]
         [:div {:class "section"}
          [:div {:class "container"}
           [:div {:class "columns"} [:div {:class "column"}]
            [:div {:class "column has-text-centered"}
             [:h1 {:class "title"} (str "Game On " playertype)]
             [:div {:class "fixed-grid has-3-cols"} (board-to-fragment board nil)]
             [:div {:id "status"}]
             [:div {:id "endedbutton"}]]
            [:div {:class "column"}]]]]])))

;; Connection management
;

(defn stream-handler [game-id playertype stream]
  (state/ensure-init-board! game-id)
  (state/clean-stream! game-id status-message playertype)
  (state/add-stream! game-id playertype stream)
  (stream/broadcast @state/all-streams status-message board-message game-id))

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
      (new js/Response (render-to-string [:html head-part welcome-page]) #js{:headers #js{:content-type "text/html"}})
      "/game"
      (let [url-game-id (.get params "game_id")]
        (new js/Response (render-to-string [:html head-part (game-page url-game-id)]) #js{:headers #js{:content-type "text/html"}}))
      "/actions/toggle"
      (let [current-player (get-in @state/all-streams [game-id :player])
            url_cell_id (parse-long (or (.get params "cell_id") ""))]
        (when (= playertype current-player)
          (state/update-board! game-id url_cell_id playertype)
          (let [board (get-in @state/all-streams [game-id :board])
                winner (check-win board)]
            (if winner
              (state/end-game! game-id status-message game-end-message end-button winner)
              (do
                (state/toggle-player! game-id)
                (stream/broadcast @state/all-streams status-message board-message game-id)))))
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
