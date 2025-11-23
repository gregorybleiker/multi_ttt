(ns multittt.stream
(:require [cljs.pprint :refer [pprint]])
)

(defn send-message [stream message]
  (try
    (.patchElements stream message)
    true
    (catch js/Error _e (let [_ (println _e)] false))))

(defn send-signal [stream signals]
  (try
    (.patchSignals stream signals)
    true
    (catch js/Error _e (let [_ (println _e)] false))))

(defn transfer [session element content ]
    (.push session #js{:elem element :hic (with-out-str (pprint content))} "render-element"))

