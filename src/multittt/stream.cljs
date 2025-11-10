(ns multittt.stream)

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

(defn broadcast [state status-message board-message game-id]
  (let [player (get-in state [game-id :player])
        board (get-in state [game-id :board])
        streams (get-in state [game-id :streams])]
    (doseq [s (map second streams)]
      (println (str (js/JSON.stringify s)))
      (send-signal s "{'test_hiccup': '[:p \"woooorks!\"]'}")
      (send-message s (status-message (str "waiting for " player)))
      (send-message s (board-message board)))))

