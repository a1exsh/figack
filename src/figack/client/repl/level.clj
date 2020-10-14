(ns figack.client.repl.level
  (:require [figack.client.repl.level
             [render :as render :refer [Render]]
             [beings :as beings]])
  (:import [figack.level Field]))

(defn render-field [field]
  (if-let [obj (->> field :objects first second)]
    (render/render obj)
    \.))

(extend-type Field
  Render
  (render [this]
    (render-field this)))
