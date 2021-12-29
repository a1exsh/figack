(ns figack.level
  (:require [figack.field :as field]))

(def width  40)
(def height 10)

#?(:clj
   (defn make-empty-level []
     (into [] (repeatedly (* width height) field/make-ref))))

(defn get-line [fields y]
  (subvec fields
          (* y width)
          (* (inc y) width)))

(defn get-field-at [fields {:keys [x y]}]
  (nth (get-line fields y) x))

(defn random-x []
  (rand-int width))

(defn random-y []
  (rand-int height))

(defn random-pos []
  {:x (random-x)
   :y (random-y)})
