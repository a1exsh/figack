(ns figack.server.core
  (:require clojure.edn
            [ring.middleware [defaults] ;;[anti-forgery]
             ]
            ;;[ring.util.response :refer [resource-response content-type]]
            [compojure.core :refer [defroutes GET POST]]
            ;;[compojure.route :as route]
            [ring.adapter.jetty9 :as jetty]
            [ring.adapter.jetty9.websocket :as ws]
            ;;
            [figack.server.world :as world])
  (:import (org.eclipse.jetty.server Server)))

(defonce connected-ws (atom {}))

(add-watch connected-ws :connected-ws
           (fn [_ _ old new]
             (when (not= old new)
               (println (format "Connected ws change: %s" new)))))

(defn broadcast-world-snapshot
  [snapshot]
  (let [data (pr-str {:world snapshot})]
    (doseq [ws (vals @connected-ws)]
      ;;(println "sending to:" ws)
      (ws/send! ws data))))

;; defonce?
(defonce ws-handler
  {:on-connect
   (fn [ws]
     (swap! connected-ws assoc  (str ws) ws) ;; TODO: id?
     (broadcast-world-snapshot (world/make-snapshot)))

   :on-close
   (fn [ws _ _]
     (swap! connected-ws dissoc (str ws) ws))

   :on-text
   (fn [ws text-message]
     (let [message (clojure.edn/read-string text-message)
           ws-id (str ws)]
       (case (:keycode message)
         38 (world/move-player! :N) ;; TODO: which player?..
         40 (world/move-player! :S)
         37 (world/move-player! :W)
         39 (world/move-player! :E))
       (broadcast-world-snapshot (world/make-snapshot))))})

(defroutes ring-routes
  (GET "/" [] (slurp "resources/public/index.html")))

(def web-app
  ;; TODO: anti-forgery
  (ring.middleware.defaults/wrap-defaults
   ring-routes
   ring.middleware.defaults/site-defaults))

(defonce web-server (atom nil))

(defn start-web-server!
  ([]
   (start-web-server! false))
  ([join?]
   (reset! web-server (jetty/run-jetty web-app {:port 8080
                                                :join? join?
                                                :websockets {"/websockets/" ws-handler}}))))
(defn stop-web-server!
  []
  (when-let [server @web-server]
    (.stop ^Server server)
    (reset! web-server nil)))

(defn start!
  []
  (world/create-world!)
  (start-web-server!))

(defn stop!
  []
  (stop-web-server!)
  (world/destroy-world!))

(comment
  (start!)
  (stop!))
