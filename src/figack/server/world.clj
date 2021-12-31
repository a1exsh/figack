(ns figack.server.world
  (:require [figack
             [field :as field]
             [level :as level]
             [movement :as movement]]
            [figack.level
             [beings :as beings]
             [gold :as gold]
             [walls :as walls]]))

(def world (level/make-empty-level 40 10))

(defn make-snapshot []
  (dosync
   (update world :fields #(->> % (map deref) (into [])))))

(defn build-border-walls! []
  (let [{:keys [width height]} world
        first-line (level/get-line world 0)
        last-line  (level/get-line world (dec height))]
    (dosync
     (doseq [x (range width)]
       (field/add-object! (nth first-line x) walls/WE)
       (field/add-object! (nth  last-line x) walls/WE))

     (doseq [y (range 1 (dec height))
             :let [line (level/get-line world y)]]
       (field/add-object! (nth line 0)           walls/NS)
       (field/add-object! (nth line (dec width)) walls/NS)))))

;; player is not a field!
(defn make-player []
  (beings/map->Being {:class :human}))

(defn add-object-at!
  "Adds a newly created object `obj` to the field at position `pos` and
  returns the position complete with object id."
  [pos obj]
  (let [id (-> world
               (level/get-field-at pos)
               (field/add-object! obj))]
    (assoc pos :id id)))

(defn del-object-at!
  "Deletes the object from the field at position (and object id) `pos`."
  [pos]
  (-> world
      (level/get-field-at pos)
      (field/del-object! (:id pos))))

(defn get-object-at
  "Returns the object from the field at position (and object id) `pos`."
  [pos]
  (-> world
      (level/get-field-at pos)
      (field/get-object (:id pos))))

(defn create-world! []
  (build-border-walls!)
  (dosync
   (add-object-at! {:x 10 :y 5} (gold/make-gold 10))))

(defn destroy-world! []
  (dosync
   (doseq [y (range (:height world))
           x (range (:width  world))
           :let [line (level/get-line world y)]]
     (ref-set (nth line x) field/empty-field))))
