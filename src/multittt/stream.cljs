(ns multittt.stream)

(defn send-message [stream message]
  (try
    (.patchElements stream message)
    true
    (catch js/Error _e false)))

(defn broadcast [state status-message board-message game-id]
  (let [player (get-in state [game-id :player])
        board (get-in state [game-id :board])
        streams (get-in state [game-id :streams])]
    (doseq [s (map second streams)]
      (send-message s (status-message (str "waiting for " player)))
      (send-message s (board-message board)))))

