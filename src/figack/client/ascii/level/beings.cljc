(ns figack.client.ascii.level.beings
  (:require figack.level.beings
            [figack.client.ascii.level.render :as render]))

(defn which-being [being]
  (:class being))

(defmulti  render-being #'which-being)

(defmethod render-being :human [_]
  \@)

(extend-type figack.level.beings.Being
  render/Render
  (to-char [this]
    (render-being this)))
