(ns figack.level)

(def width 40)
(def height 10)

(defrecord Field [type])

(defn empty-field []
  (map->Field {}))

(defn empty-level []
  (into [] (repeatedly (* width height)
                       #(ref (empty-field)))))

(defn get-line [fields y]
  (subvec fields
          (* y width)
          (* (inc y) width)))