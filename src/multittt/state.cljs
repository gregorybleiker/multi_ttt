(ns multittt.state
  (:require [multittt.stream :as stream]))

(def
  ^{:doc "a map with the id as key and a collection of streams that subscribe to this key"}
  all-streams
  (atom (hash-map)))

(defn set-board! [game-id board]
  (swap! all-streams (fn [state] (update-in state [game-id :board] (fn [_] board)))))

(defn update-board! [game-id cell-id v]
  (let [board (get-in @all-streams [game-id :board])
        new-board (assoc board cell-id v)]
    (set-board! game-id new-board)))

(defn toggle-player! [game-id]
  (swap! all-streams (fn [state] (update-in state [game-id :player] #(if (= "X" %) "O" "X")))))

(defn ensure-init-board! [game-id]
  (swap! all-streams (fn [state]
                       (-> state
                           (update-in [game-id :board] (fnil identity (vec (repeat 9 nil))))
                           (update-in [game-id :player] (fnil identity "X"))))))

(defn add-stream! [game-id playertype stream]
  (swap! all-streams (fn [state]
                       (assoc-in state [game-id :streams playertype] stream))))

(defn clean-stream!
  "tries to send a message. If unsuccessful, removes stream from state"
  [game-id status-message playertype]
  (let [stream (get-in @all-streams [game-id :streams playertype])]
    (when-not (stream/send-message stream (status-message  "cleaning"))
      (swap! all-streams update-in [game-id :streams] dissoc playertype))))

(defn end-game! [game-id status-message game-end-message end-button winner]
  (let [board (get-in @all-streams [game-id :board])
        streams (get-in @all-streams [game-id :streams])]
    (doseq [s (map second streams)]
      (stream/send-message s (status-message (str winner " wins the game")))
      (stream/send-message s (game-end-message board winner))
      (stream/send-message s end-button))
    (swap! all-streams dissoc game-id)))
