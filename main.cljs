(ns multittt-server
  (:require ["npm:react"]
            ["npm:react-dom/server"]
            [nbb.core :refer [await]]
            ["jsr:@hono/hono" :as h]
            [reagent.core :as r]
            [reagent.dom.server :refer [render-to-string]]
            ["jsr:@gavriguy/datastar-sdk" :as datastar]))

(def headpart
  (render-to-string [:head [:script {:type "module" :src "https://cdn.jsdelivr.net/gh/starfederation/datastar@v1.0.0-beta.11/bundles/datastar.js"}]]))

(def page1
  (render-to-string [:body [:h1 "Hello Hiccup 6"]
                     [:input {:data-bind "input"}]
                     [:div {:data-text "$input.toUpperCase()"}]
                     [:button {:data-show "$input != ''"} "Save"]
                     [:div {:id "question"}]
                     [:button {:data-on-click "@get('/actions/quiz')"} "Fetch a question"]]))

(def msg (render-to-string [:div [:p "Hello rocket 3"]]))

(defn init-routes [] (let [app (new h/Hono)]
                       (.get app "/"
                             (fn [ctx]
                               (.html ctx (str headpart page1))))
                       (.get app "/actions/quiz"
                             (fn [ctx]
                               (print (await (datastar/ServerSentEventGenerator.readSignals ctx.req)))
                               (print "hello")
                               (datastar/ServerSentEventGenerator.stream (fn [stream]
                                                                           (.mergeFragments stream
                                                                                            (render-to-string [:div {:id "question"} "What is 1 + 1"]))))))

                       app.fetch))

(defonce the-server nil)
(defn start-server []
  (init-routes)
  (set! the-server (js/Deno.serve (init-routes))))

(defn stop-server [] (await (.shutdown the-server)))
