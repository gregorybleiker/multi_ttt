(ns multittt-server
  (:require ["npm:react"]
            ["npm:react-dom/server"]
            [nbb.core :refer [await]]
            [promesa.core :as p]
            ["jsr:@hono/hono" :as h]
            [reagent.core :as r]
            [reagent.dom.server :refer [render-to-string]]
            ["jsr:@gavriguy/datastar-sdk" :as datastar]))

(defn sleep [ms]
  (js/Promise.
   (fn [resolve _]
     (js/setTimeout resolve ms))))

(def headpart
  (render-to-string [:head [:script {:type "module" :src "https://cdn.jsdelivr.net/gh/starfederation/datastar@v1.0.0-beta.11/bundles/datastar.js"}]]))

(def my-question (atom {:question "What is 1+2"}))

(def page1
  (render-to-string [:body [:h1 "Hello Hiccup 6"]
                     [:input {:data-bind "input"}]
                     [:div {:data-text "$input.toUpperCase()"}]
                     [:button {:data-show "$input != ''"} "Save"]
                     [:div {:id "question"}]
                     [:button {:data-on-click "@get('/actions/quiz')"} "Fetch a question"]]))

(add-watch my-question :w (fn [a b c d] (prn "this: " d)))

(defn init-routes [] (let [app (new h/Hono)]
                       (.get app "/"
                             (fn [ctx]
                               (.html ctx (str headpart page1))))
                       (.get app "/actions/quiz"
                             (^:async (fn [ctx]
                               ;(dotimes [i 10]
                                        (.stream datastar/ServerSentEventGenerator
                                                 (fn [stream]
                                                   (.mergeFragments stream (render-to-string [:div {:id "question"} (str "1 + " 5)]))
                                                   (dotimes [_ 1000000] _)))
                                        {:keepalive true}))
                             ;)
                             )
                       app))

(defonce the-server nil)

(defn start-server []
  (let [r (init-routes)]
    (set! the-server (js/Deno.serve r.fetch))))

(defn stop-server [] (await (.shutdown the-server)))

(defn restart []
  (await (stop-server))
  (start-server))
