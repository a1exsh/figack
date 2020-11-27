(ns ^:figwheel-hooks figack.core
  (:require
   [goog.dom :as gdom]
   [figack.client.web :as client]))

(println "This text is printed from src/figack/core.cljs.")

;; define your app data so that it doesn't get over-written on reload
(defonce app-state (atom {:text "Hello world!"}))

(defn get-app-element []
  (gdom/getElement "app"))

(defn render []
  (let [app (get-app-element)]
    (gdom/setTextContent app (@app-state :text))))

;; specify reload hook with ^;after-load metadata
(defn ^:after-load on-reload []
  (render))

(defonce start-once_ (client/start!))
