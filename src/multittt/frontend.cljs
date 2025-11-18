(ns multittt.frontend
  (:require
   [reagent.dom.server :refer [render-to-string render-to-static-markup]]
   [multittt.hiccuper :as h]))

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

(defn my-alert [] (js/alert "you clicked") (set! (.-my-alert js/window) my-alert))

(def testscittle [:script {:type "application/x-scittle" :data-text "
      (require '[replicant.string :as s]
      '[replicant.dom :as r])
      (def el (js/document.getElementById \"replicanttest\"))
      (r/render el $test_hiccup)
"}])

(def renderelem [:script {:type "application/x-scittle"} " 
      (require
      '[replicant.string :as s]
      '[clojure.edn :as edn]
      '[replicant.dom :as r])
      (println \"I'm loading myself\")
      (defn renderelm [elem hic]

      (def el (js/document.getElementById elem))
      (println elem)
      (println hic)
      (r/render el (edn/read-string hic))
      \"\"
       )      
      (set! (.-renderelem js/window) renderelm)

      "])


(def head-part
  [:head
   [:script {:type "application/javascript" :src "https://cdn.jsdelivr.net/npm/scittle@0.7.28/dist/scittle.min.js"}]
   [:script {:type "application/javascript" :src "https://cdn.jsdelivr.net/npm/scittle@0.7.28/dist/scittle.replicant.js"}]
   [:script "var SCITTLE_NREPL_WEBSOCKET_PORT = 1340"]
;   [:script {:type "application/javascript" :src "https://cdn.jsdelivr.net/npm/scittle@0.7.28/dist/scittle.nrepl.js"}]
   renderelem
   [:link {:rel "stylesheet" :href  "https://cdn.jsdelivr.net/npm/bulma@1.0.4/css/bulma.min.css"}]
;;   [:script {:type "module"} "

  ; scittle.core.disable_auto_eval();
  ; await scittle.core.eval_script_tags();
  ; await import(\"https://cdn.jsdelivr.net/gh/starfederation/datastar@main/bundles/datastar.js\");
 ; "]])
  [:script {:type "module" :src "https://cdn.jsdelivr.net/gh/starfederation/datastar@1.0.0-RC.6/bundles/datastar.js"}]])


(def starter-page [:body {:id "startingpoint" :data-attr:dummy "el.id" :data-init "@get('/connect')"}])

(def welcome-page
  [:body
   [:div  {:data-signals "{game_id: '', test_hiccup: '[:p \"iiiiihiii\"]'}"}]
   [:section {:class "section"}
    [:div {:class "container has-text-centered"}
     [:h1 {:class "title"} "Start a Game"]
     [:div {:class "block"}
      [:input {:data-bind "game_id"}]]
     [:div {:class "block"}
      [:button {:class "button" :data-show "$game_id != ''"
                :data-on-click "@get( '/actions/redirect?url=' + encodeURI('/game?game_id=' + $game_id.toUpperCase()))"}
       [:span {:data-text "'Start Game ' + $game_id.toUpperCase()"}]]]
     [:div {:display "none" :data-text "window.renderelem('replicanttest', $test_hiccup)"}]
     [:div {:id "replicanttest"}]
   [:button {:onclick "my_alert()"} "clickme"]]]])

(def homepage (h/hiccup->document [:html head-part starter-page]))

(defn game-page [streams game-id]
  (let [playertype (if-not (get-in streams [game-id :streams "X"]) "X"
                           (if-not (get-in streams [game-id :streams "O"]) "O"
                                   "Full"))
        board (get-in streams [game-id :board])]
    (if (= playertype "Full") [:body [:h1 "Sorry, we're full"]]
        [:body
         [:div  {:data-signals (str "{test_hiccup: '[:p \"hello\" ]',  game_id:" (to-js game-id) ", playertype: " (to-js playertype) ", board:'[]'}")}]
         [:div {:data-on-load "@get('/actions/connect')"}]
         [:div {:class "section"}
          [:div {:class "container"}
           [:div {:class "columns"} [:div {:class "column"}]
            [:div {:class "column has-text-centered"}
             [:h1 {:class "title"} (str "Game On " playertype)]
             [:div {:class "fixed-grid has-3-cols"} (board-to-fragment board nil)]
             [:div {:id "status"}]
             [:div {:id "endedbutton"}]]
            [:div {:class "column"}]]]]
     [:div {:display "none" :data-text "window.renderelem('replicanttest', $test_hiccup)"}]
     [:div {:id "replicanttest"}]
            ])))

(defn gamepage [streams game-id] (render-to-string [:html head-part (game-page streams game-id)]))
