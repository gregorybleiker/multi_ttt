(ns multittt.frontend
(:require 
  [reagent.dom.server :refer [render-to-string]]))
  
; frontend related
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

(defn game-page [streams game-id]
  (let [playertype (if-not (get-in streams [game-id :streams "X"]) "X"
                           (if-not (get-in streams [game-id :streams "O"]) "O"
                                   "Full"))
        board (get-in streams [game-id :board])]
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
