(ns ^:figwheel-hooks figack.core
  (:require
   [goog.dom :as gdom]))

(println "This text is printed from src/figack/core.cljs.")

(defn multiply [a b] (* a b))


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

;; a "meh" to this approach:
(defonce start-up
  (do
    (render)
    ;; maybe do something else here as well
    ))
