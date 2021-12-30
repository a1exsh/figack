(ns figack.level
  (:require [figack.field :as field]))

#?(:clj
   (defn make-empty-level [width height]
     {:width  width
      :height height
      :fields (into [] (repeatedly (* width height) field/make-ref))}))

(defn get-line [{:keys [width fields]} y]
  (subvec fields
          (* y width)
          (* (inc y) width)))

(defn get-field-at [level {:keys [x y]}]
  (nth (get-line level y) x))

(defn random-x [{width :width}]
  (rand-int width))

(defn random-y [{height :height}]
  (rand-int height))

(defn random-pos [level]
  {:x (random-x level)
   :y (random-y level)})
