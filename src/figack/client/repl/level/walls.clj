(ns figack.client.repl.level.walls)

(defn which-wall [w]
  (:dir w))

(defmulti render-wall #'which-wall)

(defmethod render-wall :WE [_]
  \-)

(defmethod render-wall :NS [_]
  \|)
