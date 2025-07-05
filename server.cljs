(ns server
  (:require ["npm:react"]
            ["npm:react-dom/server"]
            [nbb.core :refer [await]]
            [clojure.string :as s]
            [reagent.dom.server :refer [render-to-string]]
            ["jsr:@gavriguy/datastar-sdk" :as d]
            [promesa.core :as p]
            [applied-science.js-interop :as j]))

;; for lit
;; see also https://github.com/starfederation/datastar/issues/356
(def headpart
  [:head
   [:script {:type "module" :src "https://cdn.jsdelivr.net/gh/starfederation/datastar@v1.0.0-beta.11/bundles/datastar.js"}]
   [:script {:type "module"} "
import {css, html, LitElement} from 'https://cdn.jsdelivr.net/gh/lit/dist@3/core/lit-core.min.js'
export class SimpleGreeting extends LitElement {
  static properties = {
    name: {type: String},
  };

  static styles = css`p { color: blue }`;

  render() {
    return html`<p>Hello, ${this.name}!</p><span data-text='$input'></span`;
  }
}
customElements.define('simple-greeting', SimpleGreeting);"]

   [:link {:rel "stylesheet" :href "https://cdn.jsdelivr.net/npm/@picocss/pico@2/css/pico.min.css"}]])

(def welcomepage
  [:body [:h1 "Start A Game"]
   [:div  {:data-signals "{clientState: {connected: false, clientid: ''}, input: ''}" :data-persist__session "input"}]
   [:input {:data-bind "input"}]
   [:button {:data-show "$input != '' && $clientState.connected==false"
             :data-on-click "@get('/actions/redirect')"}
    [:span {:data-text "'Start Game ' + $input.toUpperCase()"}]]
   ])

(defn gamepage [s]
  [:body [:h1 "Game On"]
   [:div  {:data-signals (js/JSON.stringify (j/get-in s [:signals]))}]
   [:simple-greeting {:data-attr "{name: $input}"}]
   [:span {:data-text "$input"}]])

;; Connection management
(def
  ^{:doc "a map with the id as key and a collection of streams that subscribe to this key"}
  all-streams
  (atom (hash-map)))

(defn add-stream [state id stream]
  (update state id (fn [v] (if v (conj v stream) #{stream}))))

(defn remove-stream [state id stream]
  (update state id (fn [v] (disj v stream))))

(defn streamhandler [id stream]
  (swap! all-streams add-stream id stream)
  (try
    (.mergeSignals stream "{clientState: {connected: true}}")
    (.mergeFragments stream (str "<div id=\"clientid\">hi there " id "</div>"))
    (catch js/Object e
      (.log js/console e))))

(defn get-signal [signals name]
  (j/get-in signals [:signals name]))

(defn routefn [req]
  (p/let [url (new js/URL req.url)
          path url.pathname
          params url.searchParams
          signals (.readSignals d/ServerSentEventGenerator req)]
    (case path
      "/"
      (new js/Response (render-to-string [:html headpart (eval welcomepage)]) #js{:headers #js{:content-type "text/html"}})
      "/game"
      (new js/Response (render-to-string [:html headpart (eval (gamepage signals))]) #js{:headers #js{:content-type "text/html"}})
      "/actions/connect"
      (.stream d/ServerSentEventGenerator
               (partial streamhandler (get-signal signals "input"))
               #js{:keepalive true})
      "/actions/redirect"
      (let [_ (prn "redirect")]
        (.stream d/ServerSentEventGenerator
                 (fn [stream] (.executeScript stream (str "setTimeout(() => window.location = '/game?" params  "')"))
                                              ;(.mergeSignals stream signals)
                   (prn (str params)))
                 #js{:keepalive true}))
      (new js/Response ")nope"))))

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

;; Utility/Testing functions

(defn sendmsg [message stream]
  (try
    (.mergeFragments stream (str "<div id='streamcontent'>" message "</div>"))
    true
    (catch js/Error _e false)))

(defn broadcast [clientid message]
  (let [successful-streams (reduce (fn [acc x]
                                     (if (sendmsg message x)
                                       (conj acc x)
                                       acc))
                                   #{}
                                   (@all-streams clientid))]

    (swap! all-streams assoc clientid successful-streams)))

(defn broadcast2 [clientid message]
  (run! (partial sendmsg message) (@all-streams clientid)))
