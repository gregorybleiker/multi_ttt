(ns server
  (:require ["npm:react"]
            ["npm:react-dom/server"]
            [nbb.core :refer [await]]
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

(defn toggle-player [game-id]
  (swap! all-streams (fn [state] (update-in state [game-id :player] (fn [old-player] (if (= 0 old-player) 1 0))))))

(defn update-board [game-id cell-id v]
  (let [board (get-in @all-streams [game-id :board])
        new-board (assoc board cell-id v)]
    (set-board game-id new-board)))

(defn ensure-init-board [game-id]
  (swap! all-streams (fn [state]
                       (-> state
                           (update-in [game-id :board] (fnil identity (vec (repeat 9 -1))))
                           (update-in [game-id :player] (fnil identity 0))))))

(defn add-stream [id stream]
  (swap! all-streams (fn [state]
                       (update-in state [id :streams] (fn [v] (if v (conj v stream) #{stream}))))))

;; for lit
;; see also https://github.com/starfederation/datastar/issues/356
(def headpart
  [:head
   [:script {:type "module" :src "https://cdn.jsdelivr.net/npm/@starfederation/datastar@1.0.0-beta.11/dist/datastar.min.js"}]
   [:link {:rel "stylesheet" :href  "https://cdn.jsdelivr.net/npm/bulma@1.0.4/css/bulma.min.css"}]])

(def welcomepage
  [:body
   [:div  {:data-signals "{clientState: {connected: false, clientid: ''}, game_id: ''}"}]
   [:section {:class "section"}
    [:div {:class "container has-text-centered"}
     [:h1 {:class "title"} "Start A Game"]
     [:div {:class "block"}
     [:input {:data-bind "game_id"}]]
     [:div {:class "block"}
     [:button {:class "button" :data-show "$game_id != '' && $clientState.connected==false"
               :data-on-click "@get('/actions/redirect')"}
      [:span {:data-text "'Start Game ' + $game_id.toUpperCase()"}]]]]]])

(defn gamepage [game-id]
  (let [c (count (get-in @all-streams [game-id :streams]))
        playertype (case c
                     0 "First player"
                     1 "Second player"
                     "Full")]
    (if (= playertype "Full") [:body [:h1 "Sorry, we're full"]]
        [:body
         [:div  {:data-signals (str "{game_id:" (to-js game-id) ", playertype: " c ", board:'[]'}")}]
         [:div {:data-on-load "@get('/actions/connect')"}]
         [:div {:class "columns"} [:div {:class "column"}]
          [:div {:class "column has-text-centered"}
           [:h1 {:class "title"} (str "Game On " playertype)]
           [:div {:class "fixed-grid has-3-cols"}
            (into [:div {:class "grid " :id "board"}]
                  (for [x (range 0 9)] [:div {:class "cell" :data-on-click (str "@get('/actions/toggle?cell_id=" x "')")} x]))]
           [:div {:id "status"}]]
          [:div {:class "column"}]]])))

;; Connection management

(defn send-message [message board stream]
  (try
    (doto stream
      (.mergeFragments (str "<div id='status'>" message "</div>"))
      (.mergeFragments (render-to-string [:div {:id (str "cell-" 0)} "hello"]))
      (.mergeSignals  (str "{board: " (to-js board) "}")))
    true
    (catch js/Error _e false)))

(defn broadcast [game-id message]
  (let [board (get-in @all-streams [game-id :board])
        successful-streams (reduce (fn [acc x]
                                     (if (send-message message board x)
                                       (conj acc x)
                                       acc))
                                   #{}
                                   (get-in @all-streams [game-id :streams]))]

    (swap! all-streams assoc-in [game-id :streams] successful-streams)))

(defn streamhandler [game-id stream]
  (add-stream game-id stream)
  (ensure-init-board game-id)
  (let [b (get-in @all-streams [game-id :board])]
    (try
      (send-message "welcome" b stream)
      (catch js/Object e
        (.log js/console e)))))

(defn get-signal [signals name]
  (j/get-in signals [:signals name]))

(defn routefn [req]
  (p/let [url (new js/URL req.url)
          path url.pathname
          params url.searchParams
          url-game-id (.get params "game_id")
          url_cell_id (parse-long (or (.get params "cell_id") ""))
          signals (.readSignals d/ServerSentEventGenerator req)
          board (get-signal signals "board")
          game-id (get-signal signals "game_id")
          playertype (get-signal signals "playertype")]
    (case path
      "/"
      (new js/Response (render-to-string [:html headpart (eval welcomepage)]) #js{:headers #js{:content-type "text/html"}})
      "/game"
      (do (broadcast game-id "cleanup")
          (new js/Response (render-to-string [:html headpart (eval (gamepage url-game-id))]) #js{:headers #js{:content-type "text/html"}}))
      "/actions/toggle"
      (let [_ (prn (str "cellid:" url_cell_id))
            current-player (get-in @all-streams [game-id :player])]

        (when (= playertype current-player) (do
                                              (update-board game-id url_cell_id playertype)
                                              (toggle-player game-id)
                                              (broadcast game-id "updating...")))
        (new js/Response))
      "/actions/connect"
      (.stream d/ServerSentEventGenerator
               (partial streamhandler game-id)
               #js{:keepalive true})
      "/actions/redirect"
      (.stream d/ServerSentEventGenerator
               (fn [stream] (.executeScript stream (str "setTimeout(() => window.location = '/game?game_id=" (s/upper-case  game-id)  "')")))
               #js{:keepalive true})
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
