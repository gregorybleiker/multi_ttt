(ns multittt.state
)
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

