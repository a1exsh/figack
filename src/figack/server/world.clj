(ns figack.server.world
  (:require [figack
             [field :as field]
             [level :as level]
             [movement :as movement]]
            [figack.level
             [beings :as beings]
             [gold :as gold]
             [walls :as walls]]))

(def world (level/make-empty-level))

(defn make-snapshot []
  (dosync
   (into [] (map deref world))))

(defn build-border-walls! []
  (let [first-line (level/get-line world 0)
        last-line  (level/get-line world (dec level/height))]
    (dosync
     (doseq [x (range level/width)]
       (field/add-object! (nth first-line x) walls/WE)
       (field/add-object! (nth  last-line x) walls/WE))

     (doseq [y (range 1 (dec level/height))
             :let [line (level/get-line world y)]]
       (field/add-object! (nth line 0)                 walls/NS)
       (field/add-object! (nth line (dec level/width)) walls/NS)))))

;; player is not a field!
(defn make-player []
  (beings/map->Being {:class :human}))

(defonce player-pos (atom nil))

(defn add-object-at!
  "Adds a newly created object `obj` to the field at position `pos` and
  returns the position complete with object id."
  [pos obj]
  (->> obj
       (field/add-object! (level/get-field-at world pos))
       (assoc pos :id)))

(defn create-world! []
  (build-border-walls!)
  (dosync
   (add-object-at! {:x 10 :y 5} (gold/make-gold 10))
   (reset! player-pos
           (add-object-at! {:x 4 :y 3} (make-player)))))

(defn destroy-world! []
  (dosync
   (doseq [y (range level/height)
           x (range level/width)
           :let [line (level/get-line world y)]]
     (ref-set (nth line x) field/empty-field))))

(defn move-player! [dir]
  (swap! player-pos #(movement/move-object-at! world % dir)))
