(ns figack.movement
  (:require #?(:clj [figack.validations :as validations])
            [figack.field :as field]
            [figack.level :as level]))

(def allowed-directions #{:N :NE :E :SE :S :SW :W :NW})

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

;; ============================================================================
#?(:clj
   (def blocker ::blocker))

#?(:clj
   (defn blocker? [x]
     (-> x class (isa? blocker))))

#?(:clj
   (defn validate-blockers [field]
     (when (->> field field/objects (filter blocker?) count (< 1))
       (throw (Exception. "Movement is blocked by another object!")))))

#?(:clj
   (validations/set-field-validator! ::blocked? #'validate-blockers))
;; ============================================================================

#?(:clj
   (defn move-object-at!
     "Tries to move an object that should be found in the level `lev` at position
      `pos` in the direction given by `dir` and returns the new position."
     [lev pos dir]
     (let [new-pos (new-pos-for-move pos dir)
           src     (level/get-field-at lev pos)
           dst     (level/get-field-at lev new-pos)
           obj-id  (:id pos)
           obj     (get (:objects @src) obj-id)]
       (assert (some? obj) (str "No object found at position: " pos))

       (alter src update :objects dissoc obj-id)
       (alter dst update :objects assoc  obj-id obj)
       new-pos)))

#?(:cljs
   (defn move-object-at [lev pos dir]
     (let [new-pos (new-pos-for-move pos dir)
           obj-id  (:id pos)
           obj     (-> lev
                       (level/get-field-at pos)
                       (field/get-object obj-id))]
       (assert (some? obj) (str "No object found at position: " pos))
       (-> lev
           (level/update-field-at pos     field/del-object obj-id)
           (level/update-field-at new-pos field/put-object obj-id obj)))))
