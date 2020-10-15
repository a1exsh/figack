(ns figack.level)

(def width 40)
(def height 10)

(defrecord Field [objects])

(defn validate-field [field]
  (when (-> field :objects count (> 1))
    (throw (Exception. "Too many objects on one field.")))

  true)

(defn make-field []
  (map->Field {:objects {}}))

(def empty-field (make-field))

(defn make-field-ref []
  (ref empty-field :validator validate-field))

(defn make-empty-level []
  (into [] (repeatedly (* width height) make-field-ref)))

(defn get-line [fields y]
  (subvec fields
          (* y width)
          (* (inc y) width)))

(defn get-field-at [fields {:keys [x y]}]
  (nth (get-line fields y) x))

(defonce object-id (atom 0))

(defn next-object-id []
  (swap! object-id inc))

(defn add-object!
  "Adds a newly created object `obj` to the field and returns the object id."
  [field obj]
  (let [obj-id (next-object-id)]
    (alter field
           assoc-in
           [:objects obj-id]
           obj)
    obj-id))
