(ns figack.level.walls)

(def allowed-dirs #{:WE :NS})

(defrecord Wall [dir])

(defn make-wall-field [dir]
  {:pre [(contains? allowed-dirs dir)]}
  (->Wall dir))

(def WE (make-wall-field :WE))
(def NS (make-wall-field :NS))
