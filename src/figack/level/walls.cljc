(ns figack.level.walls
  (:require #?(:cljs cljs.reader
               :clj  [figack.movement :as movement])))

(def allowed-dirs #{:WE :NS})

(defrecord Wall [dir])
#?(:cljs (cljs.reader/register-tag-parser! 'figack.level.walls.Wall map->Wall))

#?(:clj (derive Wall movement/blocker))

(defn make-wall [dir]
  {:pre [(contains? allowed-dirs dir)]}
  (->Wall dir))

(def WE (make-wall :WE))
(def NS (make-wall :NS))
