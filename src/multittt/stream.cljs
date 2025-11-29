(ns multittt.stream
  (:require [cljs.pprint :refer [pprint]]))

(defn send-message [stream message]
  (try
    (.patchElements stream message)
    true
    (catch js/Error _e (let [_ (println _e)] false))))

(defn send-signal [session signals]
(prn "send-signal")
  (try
    (.push session
           (str "signals " (js/JSON.stringify signals))
           "datastar-patch-signals")
    true
    (catch js/Error _e (let [_ (println _e)] false))))

(defn transfer [session element content]
(prn "transfer")
  (.push session
         (str "renderdata " (js/JSON.stringify #js {:elem element :hic (with-out-str (pprint content))}))
         "datastar-render-element"))

