(ns figack.movement
  (:require [figack
             [level :as level]
             [validations :as validations]]))

(def allowed-directions #{:N :NE :E :SE :S :SW :W :NW})

(def blocker ::blocker)

(defn blocker? [x]
  (-> x class (isa? blocker)))

(defn validate-blockers [field]
  (when (->> field level/field-objects (filter blocker?) count (< 1))
    (throw (Exception. "Movement is blocked by another object!"))))

(validations/set-field-validator! ::blocked? #'validate-blockers)

(defn new-pos-for-move
  "Calculates the new virtual position for a given move direction.  Performs no
  boundary checks."
  [pos dir]
  {:pre [(contains? allowed-directions dir)]}
  (case dir
    :N  (-> pos                 (update :y dec))
    :NE (-> pos (update :x inc) (update :y dec))
    :E  (-> pos (update :x inc))
    :SE (-> pos (update :x inc) (update :y inc))
    :S  (-> pos                 (update :y inc))
    :SW (-> pos (update :x dec) (update :y inc))
    :W  (-> pos (update :x dec))
    :NW (-> pos (update :x dec) (update :y dec))))

(defn- report-exception! [^Exception ex]
  (-> (if (instance? java.lang.IllegalStateException ex)
        (.getCause ex)
        ex)
      .getMessage
      (or (str ex))
      println))

(defn move-object-at!
  "Tries to move an object that should be found in the level `lev` at position
  `pos` in the direction given by `dir` and returns the new position (which
  might be unchanged, in case of hitting an obstacle, or different from the
  expected position, in case of other interactions)."
  [lev pos dir]
  (try
    (let [new-pos (new-pos-for-move pos dir)
          src (level/get-field-at lev pos)
          dst (level/get-field-at lev new-pos)
          obj-id (:id pos)]
      (dosync
       (let [obj (get (:objects @src) obj-id)]
         (assert obj (str "The object must be found at the given position:" pos))
         (alter src update :objects dissoc obj-id)
         (alter dst update :objects assoc  obj-id obj)))
      new-pos)
    (catch Exception ex
      (report-exception! ex)
      pos)))
