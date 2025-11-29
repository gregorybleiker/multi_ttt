(ns multittt.server
  (:require ["npm:react"]
            ["npm:react-dom/server"]
            ["jsr:@hono/hono" :as hono]
            ["npm:@hono/node-server" :refer [serve]]
            ["npm:@starfederation/datastar-sdk/web" :as d]
            ["jsr:@mwid/better-sse" :refer [createResponse]]
            [promesa.core :as p]
            [applied-science.js-interop :as j]
            [multittt.stream :as stream]
            [multittt.frontend :as frontend]))

(defn get-signal [signals name]
  (if signals
    (j/get-in signals [:signals name])
    nil))

(defonce sessions (atom {}))

(defn init-page [sessionid]
  (let [session (get-in @sessions [sessionid :session])
        _ (prn (str "session: " session " sessionid: " sessionid ))
        initialized (get-in @sessions [sessionid :initialized])]
    (when (not initialized)
      (swap! sessions assoc-in [sessionid :initialized] true)
      (stream/transfer session "topelement" [:p "hello"])
      (stream/send-signal session #js{:tablevalue "from init"})
      )))

(defn route! [r]
  (.get r "/" (fn [c] (.html c frontend/homepage)))
  (.get r "connect" (fn [c]
                      (let [sessionid (.query c.req "sessionid")
                            sessionoptions #js{:serializer (fn [c] (let [_ (println c)]) c)}]
                        (createResponse c.req.raw sessionoptions (fn [session]
                                                                   (swap! sessions assoc-in [sessionid :session] session)
                                                                   (init-page sessionid))))))
  (.get r "*" (fn [c] (.text c "nope"))))

(defonce webserver (atom {}))
(defonce webrouter (atom {}))

(defn signalware [c next]
  (p/let [signals (.readSignals d/ServerSentEventGenerator c.req)
          has-signal (j/get signals :success)]
    (when has-signal
      (let [sessionid (get-signal signals "sessionid")
            game-id (get-signal signals "game_id")
            playertype (get-signal signals "playertype")]
        (.set c "sessionid" sessionid)
        (.set c "game-id" game-id)
        (.set c "playertype" playertype)))
    (p/do! (next))))

(defn start [port]
  (reset! webrouter (hono/Hono.))
  (.use @webrouter signalware)
  (route! @webrouter)
  (reset! webserver (serve #js {:fetch (.-fetch @webrouter) :port port})))

(defn stop []
  (.close @webserver))

(defn add-route []
  (.get @webrouter "/bla" (fn [c] (.text c "new route"))))
