(ns figack.field
  (:require #?(:cljs cljs.reader
               :clj  [figack.validations :as validations])))

(defrecord Field [objects])
#?(:cljs (cljs.reader/register-tag-parser! 'figack.field.Field map->Field))

(defn make-field []
  (map->Field {:objects {}}))

(defn objects [field]
  (->> field :objects vals))

(defn empty-field? [field]
  (->> field objects empty?))

(def empty-field (make-field))

#?(:clj
   (defn make-ref []
     (ref empty-field :validator #'validations/validate-field)))

#?(:clj
   ;; could be a ref as well, but would introduce unneeded contention
   (defonce object-id (atom 0)))

#?(:clj
   (defn next-object-id []
     (swap! object-id inc)))

#?(:clj
   (defn add-object!
     "Adds a newly created object `obj` to the field and returns the object id."
     [field obj]
     (let [obj-id (next-object-id)]
       (alter field update :objects assoc obj-id obj)
       obj-id)))

#?(:clj
   (defn del-object!
     "Deletes an object identified by its object id from the field."
     [field obj-id]
     (alter field update :objects dissoc obj-id)))

(comment
  "Maybe we should not have this function and rely on movements instead."
  #?(:clj
     (defn put-object! [field obj-id obj]
       (alter field update :objects assoc obj-id obj))))

#?(:clj
  (defn get-object [field obj-id]
    (->  @field
         (get :objects)
         (get obj-id))))

#?(:cljs
  (defn get-object [field obj-id]
    (->  field
         (get :objects)
         (get obj-id))))

#?(:cljs
  (defn put-object [field obj-id obj]
    (update field :objects assoc obj-id obj)))

#?(:cljs
  (defn del-object [field obj-id]
    (update field :objects dissoc obj-id)))

#_(let [w 2
        x 1
        y 1]
    (-> [{} {} {} {:objects {123 "xxx"}}]
        (update (+ (* y w) x) del-object 123)))
