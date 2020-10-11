(ns figack.client.repl.level.walls
  (:require [figack.client.repl.level.render :refer [Render]])
  (:import [figack.level.walls Wall]))

(defn which-wall [w]
  (:dir w))

(defmulti render-wall #'which-wall)

(defmethod render-wall :WE [_]
  \-)

(defmethod render-wall :NS [_]
  \|)

(extend-type Wall
  Render
  (render [this]
    (render-wall this)))
