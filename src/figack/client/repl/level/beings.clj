(ns figack.client.repl.level.beings)

(defn which-being [being]
  (:class being))

(defmulti render-being #'which-being)

(defmethod render-being :human [_]
  \@)
