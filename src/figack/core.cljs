(ns ^:figwheel-hooks figack.core
  (:require
   [goog.dom :as gdom]
   [figack.client.web :as client]))

(println "This text is printed from src/figack/core.cljs.")

(defn ^:after-load on-reload []
  #_(client/take-world-snapshot! 5000))

(defonce start-once_ (client/start!))
