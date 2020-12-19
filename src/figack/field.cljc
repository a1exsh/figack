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
