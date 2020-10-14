(ns figack.client.repl.level.beings
  (:require [figack.client.repl.level
             [render :refer [Render]]])
  (:import [figack.level.beings Being]))

(defn which-being [being]
  (:class being))

(defmulti render-being #'which-being)

(defmethod render-being :human [_]
  \@)

(extend-type Being
  Render
  (render [this]
    (render-being this)))
