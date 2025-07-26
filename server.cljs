(ns server
  (:require ["npm:react"]
            ["npm:react-dom/server"]
            [clojure.string :as s]
            [reagent.dom.server :refer [render-to-string]]
            ["npm:datastar-sdk/web" :as d]
            [promesa.core :as p]
            [applied-science.js-interop :as j]))

(defn to-js [s] (js/JSON.stringify (clj->js s)))

(def
  ^{:doc "a map with the id as key and a collection of streams that subscribe to this key"}
  all-streams
  (atom (hash-map)))

(defn set-board [game-id board]
  (swap! all-streams (fn [state] (update-in state [game-id :board] (fn [_] board)))))

;; winning validation
(defn all-same [arr]
  (when (apply = arr) (first arr)))

(defn check-win [game-id] (let [board (get-in @all-streams [game-id :board])
                                ;; combinations are the subsets of the board which are 3 in a row for tic tac toe
                                combinations (into [] (map #(map board %) ['(0 1 2) '(3 4 5) '(6 7 8) '(0 3 6) '(1 4 7) '(2 5 8) '(0 4 8) '(2 4 6)]))]
                            (first (keep identity (map all-same combinations)))))

(defn board-to-fragment [board winner]
  (into [:div {:class "grid" :id "board"}]
        (for [x (range 0 9)]
          (let [value (get board x)
                normalclass "cell is-flex is-align-items-center is-justify-content-center is-size-1"
                winnerclass (if (and (= winner "X") (= value "X")) "is-underlined"
                                (if (and (= winner "O") (= value "O")) "is-underlined" ""))
                classes (str normalclass winnerclass)]
            [:div {:class classes :style {:border "1px solid" :aspect-ratio "1"} :id (str "cell-" x) :data-on-click (str "@get('/actions/toggle?cell_id=" x "')")} value]))))

(defn next-player [old-player]
  (if (= "X" old-player) "O" "X"))

(defn toggle-player [game-id]
  (swap! all-streams (fn [state] (update-in state [game-id :player] next-player))))

(defn update-board [game-id cell-id v]
  (let [board (get-in @all-streams [game-id :board])
        new-board (assoc board cell-id v)]
    (set-board game-id new-board)))

(defn ensure-init-board [game-id]
  (swap! all-streams (fn [state]
                       (-> state
                           (update-in [game-id :board] (fnil identity (vec (repeat 9 nil))))
                           (update-in [game-id :player] (fnil identity "X"))))))

(defn add-stream [game-id playertype stream]
  (swap! all-streams (fn [state]
                       (assoc-in state [game-id :streams playertype] stream))))

(def headpart
  [:head
   [:script {:type "module" :src "https://cdn.jsdelivr.net/npm/@starfederation/datastar@1.0.0-beta.11/dist/datastar.min.js"}]
   [:link {:rel "stylesheet" :href  "https://cdn.jsdelivr.net/npm/bulma@1.0.4/css/bulma.min.css"}]])

(def welcomepage
  [:body
   [:div  {:data-signals "{game_id: ''}"}]
   [:section {:class "section"}
    [:div {:class "container has-text-centered"}
     [:h1 {:class "title"} "Start A Game"]
     [:div {:class "block"}
      [:input {:data-bind "game_id"}]]
     [:div {:class "block"}
      [:button {:class "button" :data-show "$game_id != ''"
                :data-on-click "@get( '/actions/redirect?url=' + encodeURI('/game?game_id=' + $game_id.toUpperCase()))"}
       [:span {:data-text "'Start Game ' + $game_id.toUpperCase()"}]]]]]])

(defn gamepage [game-id]
  (let [playertype (if-not (get-in @all-streams [game-id :streams "X"]) "X"
                           (if-not (get-in @all-streams [game-id :streams "O"]) "O"
                                   "Full"))
        board (get-in @all-streams [game-id :board])]
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

(defn status-message [text] (str "<div id='status'>" text "</div>"))
(defn board-message [board] (render-to-string (board-to-fragment board nil)))
(defn game-end-message [board winner] (render-to-string (board-to-fragment board winner)))
(def end-button (render-to-string [:button {:class "button" :data-on-click "@get('/actions/redirect?url='+encodeURI('/'))" :id "endedbutton"} "restart"]))

;; Connection management

(defn send-message [stream message]
  (try
    (.mergeFragments stream message)
    true
    (catch js/Error _e false)))

(defn clean-stream [game-id playertype]
  (let [board (get-in @all-streams [game-id :board])
        stream (get-in @all-streams [game-id :streams playertype])]
    (when-not (send-message stream (status-message  "cleaning"))
      (swap! all-streams update-in [game-id :streams] dissoc playertype))))

(defn broadcast [game-id]
  (let [player (get-in @all-streams [game-id :player])
        board (get-in @all-streams [game-id :board])
        streams (get-in @all-streams [game-id :streams])]
    (doseq [s (map second streams)]
      (send-message s (status-message (str "waiting for " player)))
      (send-message s (board-message board)))))

(defn end-game [game-id winner]
  (let [board (get-in @all-streams [game-id :board])
        streams (get-in @all-streams [game-id :streams])]
    (doseq [s (map second streams)]
      (send-message s (status-message (str winner " wins the game")))
      (send-message s (game-end-message board winner))
      (send-message s end-button))
    (swap! all-streams dissoc game-id)))

(defn streamhandler [game-id playertype stream]
  (ensure-init-board game-id)
  (clean-stream game-id playertype)
  (add-stream game-id playertype stream)
  (broadcast game-id))

(defn get-signal [signals name]
  (j/get-in signals [:signals name]))

(defn routefn [req]
  (p/let [url (new js/URL req.url)
          path url.pathname
          params url.searchParams
          url_cell_id (parse-long (or (.get params "cell_id") ""))
          signals (.readSignals d/ServerSentEventGenerator req)
          board (get-signal signals "board")
          game-id (get-signal signals "game_id")
          playertype (get-signal signals "playertype")]
    (case path
      "/"
      (new js/Response (render-to-string [:html headpart welcomepage]) #js{:headers #js{:content-type "text/html"}})
      "/game"
      (let [url-game-id (.get params "game_id")]
        (new js/Response (render-to-string [:html headpart (gamepage url-game-id)]) #js{:headers #js{:content-type "text/html"}}))
      "/actions/toggle"
      (let [current-player (get-in @all-streams [game-id :player])]
        (when (= playertype current-player) (do
                                              (update-board game-id url_cell_id playertype)
                                              (if-let [winner (check-win game-id)]
                                                (end-game game-id winner)
                                                (do
                                                  (toggle-player game-id)
                                                  (broadcast game-id)))))
        (new js/Response))
      "/actions/connect"
      (.stream d/ServerSentEventGenerator
               (partial streamhandler game-id playertype)
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

(defn start-server []
  (set! the-server (js/Deno.serve routefn)))

(defn stop-server [] (.shutdown the-server))

(defn restart []
  (p/do!
   (reset! all-streams (hash-map))
   (stop-server)
   (start-server)
   ;; important: last expr should not be a promise, so fn returns only after all promises above are resolved
   (prn "restarted")))
