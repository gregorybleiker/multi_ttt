(ns multittt-server
  (:require ["npm:react"]
            ["npm:react-dom/server"]
            [nbb.core :refer [await]]
            ["jsr:@hono/hono" :as h]
            [reagent.core :as r]
            [reagent.dom.server :refer [render-to-string]]))

;(def app (new h/Hono))

(def datastar-script "https://cdn.jsdelivr.net/gh/starfederation/datastar@v1.0.0-beta.11/bundles/datastar.js")

(def headpart
  (render-to-string [:head [:script {:type "module" :src datastar-script}]]))

(def page1
  (render-to-string [:html [:head [:script {:type "module" :src datastar-script}]] [:body [:h1 "Hello Hiccup 5"]
                                                                                    [:input {:data-bind "input"}]
                                                                                    [:div {:data-text "$input"}]]]))

(def msg (render-to-string [:div [:p "Hello rocket 3"]]))

(defn init-routes [] (let [app (new h/Hono)]
                       (.get app "/"
                             (fn [ctx]
                               (.html ctx (str headpart page1))))
                       (.get app "/hello"
                             (fn [ctx]
                               (.html ctx (str headpart page1))))
                       app.fetch))

(defonce the-server nil)
(defn start-server []
  (init-routes)
  (set! the-server (js/Deno.serve (init-routes))))

(defn stop-server [] (await (.shutdown the-server)))
