(ns figack.client.repl.level
  (:require [figack.client.repl.level
             [render :refer [Render]]
             [beings :as beings]])
  (:import [figack.level Field]))

(defn render-field [field]
  (if-let [being (:being field)]
    (beings/render-being being)
    \.))

(extend-type Field
  Render
  (render [this]
    (render-field this)))
