(ns figack.level.beings
  (:require #?(:cljs cljs.reader
               :clj  [figack.movement :as movement])))

(defrecord Being [class])
#?(:cljs (cljs.reader/register-tag-parser! 'figack.level.beings.Being map->Being))

#?(:clj (derive Being movement/blocker))
