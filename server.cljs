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
  [:head
   [:script {:type "module" :src "https://cdn.jsdelivr.net/gh/starfederation/datastar@v1.0.0-beta.11/bundles/datastar.js"}]
   [:script {:type "module"} "
import {css, html, LitElement} from 'https://cdn.jsdelivr.net/gh/lit/dist@3/core/lit-core.min.js'
export class SimpleGreeting extends LitElement {
  static properties = {
    name: {type: String},
    data: {attribute: false},
  };

  static get styles() {
    return css`p { color: blue }`;
  }

  render() {
    return html`<p>Hello, ${this.name}!</p>`;
  }
}
customElements.define('simple-greeting', SimpleGreeting);"]

   [:link {:rel "stylesheet" :href "https://cdn.jsdelivr.net/npm/@picocss/pico@2/css/pico.min.css"}]])

(def page1
  [:body [:h1 "Connect to a Stream"]
   [:div  {:data-signals "{clientState: {connected: false, clientid: ''}, input: ''}"}]
   [:simple-greeting {:data-attr "{name: $input}"}] ;;" :id "greeting"}]

   [:input {:data-bind "input"}]
   [:button {:data-show "$input != '' && $clientState.connected==false"
             :data-on-click "@get('/actions/connect')"}
    [:span {:data-text "'Connect ' + $input.toUpperCase()"}]]
   [:div {:id "clientid" :data-attr "{blu: $input}"}]
   [:div {:id "streamcontent"}]])

(defonce counter (atom 0))

(defonce the-server nil)

(defonce  all-streams (atom (hash-map)))

(defn streamhandler [stream]
  (let [clientid (random-uuid)]
    (swap! all-streams assoc clientid stream)

    (js/console.log (str "adding to stream " @counter))
    (swap! counter inc)
    (try
      (.mergeSignals stream "{clientState: {connected: true}}")
      (.mergeFragments stream (str "<div id=\"clientid\">hi there " clientid "</div>"))
      (catch js/Object e
        (.log js/console e)))))

(defn createhandler [req]
  streamhandler)

(defn routefn [req]
  (let [url (new js/URL req.url)
        path url.pathname]
    (case path
      "/" (new js/Response (render-to-string [:html headpart (eval page1)]) #js{:headers #js{:content-type "text/html"}})
      "/actions/connect" (.stream d/ServerSentEventGenerator (createhandler req) #js{:keepalive true})
      (new js/Response "nope"))))

(defn start-server []
  (set! the-server (js/Deno.serve routefn)))

(defn stop-server [] (.shutdown the-server))

(defn restart []
  (p/do!
          (stop-server)
          (start-server)))

(defn sendmsg [message n]
  (try
    (.mergeFragments (second n) (str "<div id='streamcontent'>" message "</div>"))
    true?
    (catch js/Object _ false)))

(defn broadcast [message]
  (reset! all-streams (into {} (filter (partial sendmsg message)
                                       @all-streams))))
