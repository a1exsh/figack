(ns figack.level.walls
  (:require [figack.movement :as movement]))

(def allowed-dirs #{:WE :NS})

(defrecord Wall [dir])

(derive Wall movement/blocker)

(defn make-wall [dir]
  {:pre [(contains? allowed-dirs dir)]}
  (->Wall dir))

(def WE (make-wall :WE))
(def NS (make-wall :NS))
