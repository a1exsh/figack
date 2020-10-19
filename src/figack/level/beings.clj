(ns figack.level.beings
  (:require [figack.movement :as movement]))

(defrecord Being [class])

(derive Being movement/blocker)
