(ns multittt.stream
(:require [multittt.state :as state]))

(defn send-message [stream message]
  (try
    (.patchElements stream message)
    true
    (catch js/Error _e false)))

(defn broadcast [game-id]
  (let [player (get-in @state/all-streams [game-id :player])
        board (get-in @state/all-streams [game-id :board])
        streams (get-in @state/all-streams [game-id :streams])]
    (doseq [s (map second streams)]
      (send-message s (status-message (str "waiting for " player)))
      (send-message s (board-message board)))))

