(ns server
  (:require ["npm:react"]
            ["npm:react-dom/server"]
            [nbb.core :refer [await]]
            [reagent.dom.server :refer [render-to-string]]
            ["jsr:@gavriguy/datastar-sdk" :as d]
            [promesa.core :as p]))

(defn sleep [ms]
  (js/Promise
   (fn [resolve _]
     (js/setTimeout resolve ms))))

(def headpart
  [:head [:script {:type "module" :src "https://cdn.jsdelivr.net/gh/starfederation/datastar@v1.0.0-beta.11/bundles/datastar.js"}]])

(def my-question (atom {:question "What is 1+2"}))

(def page1
  [:body [:h1 "Hello Hiccup 6"]
   [:input {:data-bind "input"}]
   [:div {:data-text "$input.toUpperCase()"}]
   [:button {:data-show "$input != ''"} "Save"]
   [:div {:id "question"}]
   [:button {:data-on-click "@get('/actions/quiz2')"} "Fetch a question"]])

(add-watch my-question :w (fn [a b c d] (prn "this: " d)))

(defonce counter (atom 0))

(defonce the-server nil)

(defonce a-stream (atom nil))

(defn create-readable-stream []
  (let [timer (atom nil)
        start (fn [^ReadableStreamReader controller]
                (reset! timer
                        (js/setInterval
                         (fn []
                           (.enqueue controller (str "Hello, World! " @counter "\n"))
                           (swap! counter inc))

                         1000)))]
    #js{:start start
        :cancel (fn []
                  (js/clearInterval @timer))}))

(defn create-readable-event-stream []
  (let [timer (atom nil)
        start (fn [^ReadableStreamReader controller]
                (reset! timer
                        (js/setInterval
                         (fn []
                           (.mergeFragments controller (str "<div id=\"question\">" @counter "</div>"))
                           (swap! counter inc))

                         1000)))]
    #js{:start start
        :cancel (fn []
                  (js/clearInterval @timer))}))
;(defn handler [req]
;  (let [body (js/ReadableStream. #js{:start (fn [controller] (start-stream controller))
;                                       :cancel (fn [] (js/clearInterval @timer)))})]
;    (js/Response. (body.pipeThrough (js/TextEncoderStream.)) #js {"content-type" "text/plain; charset=utf-8"})))

(defn streamhandler [stream]
  (reset! a-stream stream)
  (js/console.log (str "setting stream " @counter))
  (swap! counter inc)
  (.mergeFragments stream (str "<div id=\"question\">hi " @counter "</div>"))
                                                                 ;;(await (sleep 5000))                                                                        ;;(.mergeFragments stream "<div id=\"question\">you</div>")
  )

(defn routefn [req]
  (let [url (new js/URL req.url)
        path url.pathname]

    (case path
      "/test" (new js/Response (str "<html><body>" url " " path  "</body></html") #js{:headers #js{:content-type "text/html"}})
      "/" (new js/Response "Hi")
      "/hic" (new js/Response (render-to-string [:html headpart page1]) #js{:headers #js{:content-type "text/html"}})
      "/actions/quiz2" (.stream d/ServerSentEventGenerator streamhandler #js{:keepalive true})
      "/actions/quiz" (let [body (new js/ReadableStream (create-readable-event-stream))]
                        (new js/Response (.pipeThrough body (new js/TextEncoderStream)) #js{:headers #js{:content-type "text/event-stream"
                                                                                                         :cache-control "no-cache"
                                                                                                         :connection "keep-alive"
                                                                                                         :transfer-encoding "chunked"}}))
      "/stream" (let [body (new js/ReadableStream (create-readable-stream))]
                  (new js/Response (.pipeThrough body (new js/TextEncoderStream)) #js{:headers #js{:content-type "text/event-stream"
                                                                                                   :cache-control "no-cache"
                                                                                                   :connection "keep-alive"
                                                                                                   :transfer-encoding "chunked"}}))
      (new js/Response "nope"))))

(defn start-server []
  (set! the-server (js/Deno.serve routefn)))

(defn stop-server [] (.shutdown the-server))

(defn restart []
  (await (p/do!
          (stop-server)
          (start-server))))
