(ns multittt.frontend
  (:require
   [reagent.dom.server :refer [render-to-string]]
   [multittt.hiccuper :as h]))

(def loader '(do
               (require
                '[replicant.string :as s]
                '[clojure.edn :as edn]
                '[replicant.dom :as r])
               (defn render-element [elem hic]
                 (println "Element:" elem)
                 (def el (js/document.getElementById elem))
                 (println hic)
                 (r/render el (edn/read-string hic))
                 "")
               (set! (.-renderelement js/window) render-element)))

(def renderelem [:script {:type "application/x-scittle"} (pr-str loader)])

(def connector '(do
                  (let [sessionid (clojure.core/random-uuid)
                        sessionElement (js/document.createElement "div")
                        _ (.setAttribute sessionElement "data-init" (str "@get('/connect?sessionid=" sessionid "')"))
                        _ (.setAttribute sessionElement "data-signals:sessionid" (str "'" sessionid "'"))]
                    (.appendChild js/document.body sessionElement))))

(def connect [:script {:type "application/x-scittle"} (pr-str connector)])

(def head-part
  [:head
   [:script {:type "module" :src "https://unpkg.com/@fluentui/web-components"}]
   [:script {:type "application/javascript" :src "https://cdn.jsdelivr.net/npm/scittle@0.7.28/dist/scittle.min.js"}]
   [:script {:type "application/javascript" :src "https://cdn.jsdelivr.net/npm/scittle@0.7.28/dist/scittle.replicant.js"}]
   [:script "var SCITTLE_NREPL_WEBSOCKET_PORT = 1340"]
   renderelem
   connect
   [:link {:rel "stylesheet" :href  "https://cdn.jsdelivr.net/npm/bulma@1.0.4/css/bulma.min.css"}]
   [:script {:type "module" :src "https://cdn.jsdelivr.net/gh/starfederation/datastar@1.0.0-RC.6/bundles/datastar.js"}]
   [:script {:type "module"} "
      ; import {watcher} from \"https://cdn.jsdelivr.net/gh/starfederation/datastar@1.0.0-RC.6/bundles/datastar.js\"
      globalThis.datastarWatcher = watcher;
      console.log('here')
  "]
   [:script {:type "module"} "
  import { watcher } from 'https://cdn.jsdelivr.net/gh/starfederation/datastar@1.0.0-RC.6/bundles/datastar.js';
watcher({
  name: 'datastar-render-element',
   apply({error}, {renderdata}) {
  const {elem, hic} = JSON.parse(renderdata);
  window.renderelement(elem, hic)  }})
  "]
   [:style "
  .cell {
    border: 1px solid hsl(0, 0%, 86%);
  }
  "]])

(def samplecomponent [:div {:class "tablecontainer"} [:table {:class "table"} [:tr] [:td {:data-text "$tablevalue"}]]])

(def demo-grid [:section {:class "section is-flex is-justify-content-center is-align-items-center" :style "min-height: 100vh;"}
                [:div {:class "fixed-grid has-3-cols" :style "width: 90%"}
                 [:div {:class "grid"}
                  (for [x (range 9)]
                    [:div {:class "cell"}
                     [:div {:class "is-flex is-justify-content-center is-align-items-center"}
                      [:div {:id (str "elem" x)} (str ".." x "..")]]])]]])

(def starter-page [:body
                   [:div {:data-signals "{initialized: 'false', tablevalue: 'abcd'}"

                          :hidden true}]
                   [:div {:id "topelement"}]
                   samplecomponent])

(def homepage (h/hiccup->document [:html head-part starter-page]))
