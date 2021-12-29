(ns figack.server.core
  (:require clojure.edn
            [clojure.pprint :refer [pprint]]
            [ring.middleware [defaults] ;;[anti-forgery]
             ]
            ;;[ring.util.response :refer [resource-response content-type]]
            [compojure.core :refer [defroutes]]
            [compojure.route :as route]
            [ring.adapter.jetty9 :as jetty]
            [ring.adapter.jetty9.websocket :as ws]
            ;;
            [figack.level :as level]
            [figack.movement :as movement]
            [figack.server.world :as world])
  (:import (org.eclipse.jetty.server Server)))

(defonce players (atom {}))

(add-watch players :debug
           (fn [_ _ old new]
             (when (not= old new)
               (print "Players change:")
               (pprint new))))

(defn broadcast-world-snapshot
  [snapshot]
  (let [data (pr-str {:world snapshot})]
    (doseq [ws (->>  @players vals (map :ws))]
      (println "sending to:" ws)
      (ws/send! ws data))))

;; defonce?
(defonce ws-handler
  {:on-connect
   (fn [ws]
     (println (format "on-connect: %s" ws))
     (let [wsk (str ws)
           pos (dosync
                (world/add-object-at! (level/random-pos) (world/make-player)))]
       (swap! players assoc wsk {:ws ws
                                 :pos (agent pos)}))
     (broadcast-world-snapshot (world/make-snapshot)))

   :on-close
   (fn [ws _ _]
     (println (format "on-close: %s" ws))
     (let [wsk (str ws)
           pos (-> @players (get wsk) :pos)]
       (swap! players dissoc wsk)
       (dosync
        (world/del-object-at! @pos))))

   :on-text
   (fn [ws text-message]
     (let [wsk (str ws)
           pos (-> @players (get wsk) :pos)
           message (clojure.edn/read-string text-message)
           keycode (:keycode message) ;; TODO: keycode is a client-side concern
           dir (case keycode
                 38 :N
                 40 :S
                 37 :W
                 39 :E
                 nil)]
       (if-not dir
         (println (format "Unknown keycode: %d" keycode))
         (send pos #(dosync
                     (movement/move-object-at! world/world % dir))))
       ;; TODO: shouldn't be here
       (broadcast-world-snapshot (world/make-snapshot))))})

(defroutes ring-routes
  (route/resources "/")
  (route/not-found "<h1>Page not found</h1>\n"))

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
