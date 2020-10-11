(ns figack.client.repl.level
  (:require [figack.client.repl.level
             [beings :as beings]
             [walls :as walls]]))

(defn which-field [field]
  (:type field))

(defmulti render-field #'which-field)

(defmethod render-field :default [_]
  \?)

;; abstraction leaks
(defmethod render-field :wall [w]
  (walls/render-wall w))

(defmethod render-field nil [field]
  (if-let [being (:being field)]
    (beings/render-being being)
    \.))
