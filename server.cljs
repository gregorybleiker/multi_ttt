(ns server
  (:require ["npm:react"]
            ["npm:react-dom/server"]
            [nbb.core :refer [await]]
            [clojure.string :as s]
            [reagent.dom.server :refer [render-to-string]]
            ["jsr:@gavriguy/datastar-sdk" :as d]
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
   [:script {:type "module" :src "https://cdn.jsdelivr.net/gh/starfederation/datastar@v1.0.0-beta.11/bundles/datastar.js"}]
   [:script {:type "module" :src "https://cdn.jsdelivr.net/npm/beercss@3.11.30/dist/cdn/beer.min.js"}]
   [:script {:type  "module" :src "https://cdn.jsdelivr.net/npm/material-dynamic-colors@1.1.2/dist/cdn/material-dynamic-colors.min.js"}]
   [:link {:rel "stylesheet" :href "https://cdn.jsdelivr.net/npm/beercss@3.11.30/dist/cdn/beer.min.css"}]
;;   [:style "article {aspect-ratio: 1;}"]
   [:script {:type "module"} "
import {css, html, LitElement} from 'https://cdn.jsdelivr.net/gh/lit/dist@3/core/lit-core.min.js'
export class TicTacToeBoard extends LitElement {
  static properties = {
    board: { type: Array }
  }

  constructor() {
    super();
  }

  handleClick(event) {
    const id = event.currentTarget.dataset.cellId;
    this.board[id]='X';
    console.log(`Clicked cell ${id}`);
    const e = new CustomEvent('ticked', {bubbles: true, composed: true, detail: {cellId: id}});
    this.dispatchEvent(e);
  }

  render() {
    if(this.board){
    return html`
      <div class=\"grid\">
        ${this.board.map((value, index) => {
         let v = ' ';
         switch (value) {
               case 0: v = 'O'; break;
               case 1: v = 'X'; break;
               default: break;
              }
         return html`
          <article data-cell-id=\"${index}\"
                   style=\"aspect-ratio: 1;\"
                   class=\"s4 padding center-align middle-align extra-large-text\"
                   @click=\"${this.handleClick}\">
             ${v}
          </article>
        `})}
      </div>
      `
      }
    }

   createRenderRoot() {
    return this;
  }
}
customElements.define('tic-tac-toe-board', TicTacToeBoard);"]])

(def welcomepage
  [:body
   [:main
    [:div  {:data-signals "{clientState: {connected: false, clientid: ''}, game_id: ''}"}]
    [:div {:class "grid"}
     [:div {:class "s4"}]
     [:div {:class "s4"}
      [:article {:class "border medium no-padding"}
       [:h5 "Start A Game"]
       [:div {:class "padding absolute center middle"}
        [:input {:data-bind "game_id"}]
        [:div {:class "space"}]
        [:button {:data-show "$game_id != '' && $clientState.connected==false"
                  :data-on-click "@get('/actions/redirect')"}
         [:span {:data-text "'Start Game ' + $game_id.toUpperCase()"}]]]]]
     [:div {:class "s4"}]]]])

(defn gamepage [game_id]
  (let [c (count (get-in @all-streams [game_id :streams]))
        playertype (case c
                     0 "First player"
                     1 "Second player"
                     "Full")]
    (if (= playertype "Full") [:body [:h1 "Sorry, we're full"]]
        [:body [:h1 (str "Game On " playertype)]
         [:div  {:data-signals (str "{game_id:" (to-js game_id) ", playertype: " c ", board:'[]'}")}]
         [:div {:data-on-load "@get('/actions/connect')"}]
         [:div {:class "grid"} [:div {:class "s4"}]
          [:div {:class "s4"}
           [:tic-tac-toe-board {:data-attr "{board: '[' + $board + ']'}" :data-on-ticked "@get(`/actions/toggle?cellId=${evt.detail.cellId}`)"}]]
          [:div {:class "s4"}]]
         [:div {:id "status"}]])))

;; Connection management

(defn send-message [message board stream]
  (try
    (doto stream
      (.mergeFragments (str "<div id='status'>" message "</div>"))
      (.mergeSignals  (str "{board: " (to-js board) "}")))
    true
    (catch js/Error _e false)))

(defn broadcast [clientid message]
  (let [board (get-in @all-streams [clientid :board])
        successful-streams (reduce (fn [acc x]
                                     (if (send-message message board x)
                                       (conj acc x)
                                       acc))
                                   #{}
                                   (get-in @all-streams [clientid :streams]))]

    (swap! all-streams assoc-in [clientid :streams] successful-streams)))

(defn streamhandler [id stream]
  (add-stream id stream)
  (ensure-init-board id)
  (let [b (get-in @all-streams [id :board])]
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
          url_cell_id (parse-long (or (.get params "cellId") ""))
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
