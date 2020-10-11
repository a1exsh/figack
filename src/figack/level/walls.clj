(ns figack.level.walls)

(def allowed-dirs #{:WE :NS})

(defn make-wall-field [dir]
  {:pre [(contains? allowed-dirs dir)]}
  {:type :wall
   :dir  dir})

(def WE (make-wall-field :WE))
(def NS (make-wall-field :NS))
