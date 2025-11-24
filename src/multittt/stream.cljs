(ns multittt.stream
(:require [cljs.pprint :refer [pprint]])
)

(defn send-message [stream message]
  (try
    (.patchElements stream message)
    true
    (catch js/Error _e (let [_ (println _e)] false))))

(defn to-js [s] (js/JSON.stringify (clj->js s)))

(defn send-signal [session signals]
  (try
    (.push session "signals {\"somevalue\": 1}
    " "datastar-fetch")
    true
    (catch js/Error _e (let [_ (println _e)] false))))

(defn transfer [session element content ]
    (.push session (js/JSON.stringify #js {:elem element :hic (with-out-str (pprint content))}) "render-element"))

