(ns figack.client.repl.level
  (:require [figack.level :as level]
            [figack.client.repl.level
             [render :as render :refer [Render]]])
  (:import [figack.level Field]))

(defmulti render-object #'render/has-renderer?)

(defmethod render-object true [obj]
  (render/render obj))

(defmethod render-object false [_]
  \?)

(defmulti render-field #'level/empty-field?)

(defmethod render-field true [_]
  \.)

(defmethod render-field false [field]
  (render-object (->> field level/field-objects first)))

(extend-type Field
  Render
  (render [this]
    (render-field this)))
