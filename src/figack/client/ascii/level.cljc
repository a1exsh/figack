(ns figack.client.ascii.level
  (:require [figack.field :as field]
            [figack.level :as level]
            [figack.client.ascii.level.render :as render])
  ;(:import [figack.field Field])
  )

(defmulti render-object #'render/has-renderer?)

(defmethod render-object true [obj]
  (render/render obj))

(defmethod render-object false [_]
  \?)

(defmulti render-field #'field/empty-field?)

(defmethod render-field true [_]
  \.)

(defmethod render-field false [field]
  (render-object (->> field field/objects first)))

(extend-type figack.field.Field
  render/Render
  (render [this]
    (render-field this)))
