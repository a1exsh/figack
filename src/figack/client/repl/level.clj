(ns figack.client.repl.level
  (:require [figack.client.repl.level
             [render :as render :refer [Render]]])
  (:import [figack.level Field]))

(defmulti render-object #'render/has-renderer?)

(defmethod render-object true [obj]
  (render/render obj))

(defmethod render-object false [_]
  \?)

(defn objects [field]
  (->> field
       :objects
       vals))

(defn is-empty? [field]
  (->> field objects empty?))

(defmulti render-field #'is-empty?)

(defmethod render-field true [_]
  \.)

(defmethod render-field false [field]
  (render-object (->> field objects first)))

(extend-type Field
  Render
  (render [this]
    (render-field this)))
