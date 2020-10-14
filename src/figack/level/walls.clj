(ns figack.level.walls)

(def allowed-dirs #{:WE :NS})

(defrecord Wall [dir])

(defn make-wall [dir]
  {:pre [(contains? allowed-dirs dir)]}
  (->Wall dir))

(def WE (make-wall :WE))
(def NS (make-wall :NS))
