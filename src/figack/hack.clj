(ns figack.hack
  (:require [figack
             [level :as level]
             [movement :as movement]]
            [figack.level
             [beings :as beings]
             [gold :as gold]
             [walls :as walls]]
            [figack.client.repl :as repl]))

(def world (level/make-empty-level))

(defn make-snapshot []
  (dosync
   (into [] (map deref world))))

(defn build-border-walls! []
  (let [first-line (level/get-line world 0)
        last-line  (level/get-line world (dec level/height))]
    (dosync
     (doseq [x (range level/width)]
       (level/add-object! (nth first-line x) walls/WE)
       (level/add-object! (nth last-line  x) walls/WE))

     (doseq [y (range 1 (dec level/height))
             :let [line (level/get-line world y)]]
       (level/add-object! (nth line 0)                 walls/NS)
       (level/add-object! (nth line (dec level/width)) walls/NS)))))

;; player is not a field!
(defn make-player []
  (beings/map->Being {:class :human}))

(defonce player-pos (atom nil))

(defn add-object-at!
  "Adds a newly created object `obj` to the field at position `pos` and
  returns the position complete with object id."
  [pos obj]
  (->> obj
       (level/add-object! (level/get-field-at world pos))
       (assoc pos :id)))

(defn create-world! []
  (build-border-walls!)
  (dosync
   (add-object-at! {:x 10 :y 5} (gold/make-gold 10))
   (reset! player-pos
           (add-object-at! {:x 4 :y 3} (make-player)))))

(defn print-help []
  (println "h=help q=quit"))

(defn move-player! [dir]
  (swap! player-pos #(movement/move-object-at! world % dir)))

(defn read-and-act! []
  (case (read)
    n  (move-player! :N)
    ne (move-player! :NE)
    e  (move-player! :E)
    se (move-player! :SE)
    s  (move-player! :S)
    sw (move-player! :SW)
    w  (move-player! :W)
    nw (move-player! :NW)
    h  (print-help)
    q  ::quit
    (print-help)))

(defn play! []
  (loop []
    (repl/print-snapshot (make-snapshot))
    (when-not (= ::quit (read-and-act!))
      (recur))))

(comment
  (load "figack/hack")
  (in-ns 'figack.hack)

  (create-world!)
  (play!)
  )
