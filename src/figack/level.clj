(ns figack.level)

(def width 40)
(def height 10)

(defrecord Field [objects])

(defn empty-field []
  (map->Field {:objects {}}))

(defn empty-level []
  (into [] (repeatedly (* width height)
                       #(ref (empty-field)))))

(defn get-line [fields y]
  (subvec fields
          (* y width)
          (* (inc y) width)))

(defn get-field-at [fields {:keys [x y]}]
  (nth (get-line fields y) x))
