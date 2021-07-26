(ns figack.client.ascii.level.beings
  (:require [figack.client.ascii.level.render :as render])
  ;(:import [figack.level.beings Being])
  )

(defn which-being [being]
  (:class being))

(defmulti render-being #'which-being)

(defmethod render-being :human [_]
  \@)

(extend-type figack.level.beings.Being
  render/Render
  (render [this]
    (render-being this)))
