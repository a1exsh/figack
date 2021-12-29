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

(defonce ws->conn (atom {}))

(add-watch ws->conn :debug
           (fn [_ _ old new]
             (when (not= old new)
               (print "Connections changed: ")
               (pprint new))))

(defn broadcast-world-snapshot [snapshot]
  (let [data {:world snapshot}]
    (doseq [conn (vals @ws->conn)
            :let [{:keys [ws player]} conn
                  seqno (:seqno @player)]]
      (println "[" seqno "] sending to:" (str ws))
      (ws/send! ws (-> data
                       (assoc :seqno seqno)
                       pr-str)))))

(defn on-connect [ws]
  (let [wsk    (str ws)
        _ (println "on-connect:" wsk)
        pos    (dosync
                (world/add-object-at! (level/random-pos) (world/make-player)))
        player (agent {:seqno 1
                       :pos   pos})
        conn   {:ws     ws
                :player player}]
    (swap! ws->conn assoc wsk conn))

  ;; TODO: it should be more discrete
  (broadcast-world-snapshot (world/make-snapshot)))

(defn on-close [ws _ _]
  (let [wsk  (str ws)
        _ (println "on-close:" wsk)
        conn (get @ws->conn wsk)]
    (swap! ws->conn dissoc wsk)

    (if-some [player (:player conn)]
      (dosync
       (world/del-object-at! (:pos @player)))
      (println "No conn for ws:" wsk))

    ;; TODO: it should be more discrete
    (broadcast-world-snapshot (world/make-snapshot))))

;; ======================================================================
(defn message->action [_player message] (:action message))

(defmulti handle-action! #'message->action)

(defmethod handle-action! :move [player {dir :dir}]
  (let [res (dosync
             (-> player
                 (update :pos #(movement/move-object-at! world/world % dir))
                 (update :seqno inc)))]

    ;; TODO: it should be more discrete
    (broadcast-world-snapshot (world/make-snapshot))

    res))
;; ======================================================================

(defn on-text [ws text-message]
  (let [wsk     (str ws)
        _ (println "on-text:" wsk)
        conn    (get @ws->conn wsk)
        player  (:player conn)
        message (clojure.edn/read-string text-message)]
    (send player handle-action! message)))

(defonce ws-handler
  {:on-connect #'on-connect
   :on-close   #'on-close
   :on-text    #'on-text})

(defroutes ring-routes
  (route/resources "/")
  (route/not-found "<h1>Page not found</h1>\n"))

(def web-app
  ;; TODO: anti-forgery
  (ring.middleware.defaults/wrap-defaults
   ring-routes
   ring.middleware.defaults/site-defaults))

;; FIXME: to avoid caching problems we should use figwheel's server instead
(defonce web-server (atom nil))

(defn start-web-server!
  ([]
   (start-web-server! false))
  ([join?]
   (reset! web-server (jetty/run-jetty web-app {:port 8080
                                                :join? join?
                                                :websockets {"/websockets/" ws-handler}}))))

(defn stop-web-server! []
  (when-let [server @web-server]
    (.stop ^Server server)
    (reset! web-server nil)))

(defn start! []
  (world/create-world!)
  (start-web-server!))

(defn stop! []
  (stop-web-server!)
  (world/destroy-world!))

(comment
  (start!)
  (stop!))
