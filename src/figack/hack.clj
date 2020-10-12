(ns figack.hack
  (:require [figack.level :as level]
            [figack.level.walls :as walls]
            [figack.client.repl :as repl]))

(defonce world (level/empty-level))

(defn make-snapshot []
  (dosync
   (into [] (map deref world))))

(defn build-border-walls! []
  (let [first-line (level/get-line world 0)
        last-line  (level/get-line world (dec level/height))]
    (dosync
     (doseq [x (range level/width)]
       (ref-set (nth first-line x) walls/WE)
       (ref-set (nth last-line  x) walls/WE))

     (doseq [y (range 1 (dec level/height))
             :let [line (level/get-line world y)]]
       (ref-set (nth line 0) walls/NS)
       (ref-set (nth line (dec level/width)) walls/NS)))))

;; player is not a field!
(defn make-player []
  {:class :human})

(defonce player-pos (atom nil))

(defn create-world! []
  (build-border-walls!)
  (let [pos {:x 4
             :y 3}]
    (reset! player-pos pos)
    (dosync
     (alter (level/get-field-at world pos)
            #(assoc % :being (make-player))))))

(defn print-help []
  (println "h=help q=quit"))

(def move-dirs #{:N :NE :E :SE :S :SW :W :NW})

(defn new-pos-for-move
  "Calculates the new virtual position for a given move direction.  Performs no
  boundary checks."
  [{:keys [x y]} dir]
  {:pre [(contains? move-dirs dir)]}
  (case dir
    :N  {:x      x  :y (dec y)}
    :NE {:x (inc x) :y (dec y)}
    :E  {:x (inc x) :y      y}
    :SE {:x (inc x) :y (inc y)}
    :S  {:x      x  :y (inc y)}
    :SW {:x (dec x) :y (inc y)}
    :W  {:x (dec x) :y      y}
    :NW {:x (dec x) :y (dec y)}))

(defn move-being-at!
  "Tries to move a being that should be found in the world at position `pos` in
  the direction given by `dir` and returns the new position (which might be
  unchanged, in case of hiting an obstacle, or different from the expected
  position, in case of other interactions)."
  [pos dir]
  (let [new-pos (new-pos-for-move pos dir)
        src (level/get-field-at world pos)
        dst (level/get-field-at world new-pos)]
    (dosync
     (let [being (:being @src)]
       (alter src #(dissoc % :being))
       (alter dst #(assoc  % :being being))))
    new-pos))

(defn move-player [dir]
  (swap! player-pos #(move-being-at! % dir)))

(defn play! []
  (loop []
    (repl/print-snapshot (make-snapshot))
    (let [cmd (read)]
      (case cmd
        n  (move-player :N)
        ne (move-player :NE)
        e  (move-player :E)
        se (move-player :SE)
        s  (move-player :S)
        sw (move-player :SW)
        w  (move-player :W)
        nw (move-player :NW)
        h (print-help)
        q nil
        (print-help))
      (when-not (= 'q cmd)
        (recur)))))

(comment
  (->> (make-snapshot) repl/render-snapshot)
;; ----------------------------------------
;; |......................................|
;; |......................................|
;; |...@..................................|
;; |......................................|
;; |......................................|
;; |......................................|
;; |......................................|
;; |......................................|
;; ----------------------------------------
  )
