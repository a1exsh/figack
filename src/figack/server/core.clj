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
            [figack.server.log :as log]
            [figack.server.world :as world])
  (:import (org.eclipse.jetty.server Server)))

(defn- ws-key [ws]
  "Derives the unique key from a websocket object."
  (str ws))

;; maps websockets (by key) to player agents
(defonce ws->player (atom {}))

(add-watch ws->player :debug
           (fn [_ _ old new]
             (when (not= old new)
               (log/log "Connections changed:" (with-out-str (pprint new))))))

(defn- send-on-websocket! [{ws :ws :as player} message]
  (log/log "sending to:" (str ws) (:action message) (keys message))
  (ws/send! ws (pr-str message))
  ;; as this is an agent function we must return player, even if unchanged
  player)

(defn- send-to-player! [player message]
  ;; blocking, use send-off
  (send-off player send-on-websocket! message))

(defn- broadcast-message! [message]
  (let [players (vals @ws->player)]
    (log/log "broadcasting to" (count players) "players" (:action message) (keys message))
    (doseq [player players]
      (send-to-player! player message))))

;; ============================================================================
;; serializes the messages and performs dispatch/broadcast to connected players
(def empty-message-log '())

(defonce message-log (agent empty-message-log))

(defn- newest-message [log]
  (first log))

(defn- add-message [log data]
  ;; TODO: add timestamp and/or serial number?
  (conj log data))

(defn- log-message! [data]
  (send message-log add-message data))

(add-watch message-log :broadcast
           (fn [_ _ _ log]
             (when-some [message (newest-message log)]
               (broadcast-message! message))))

(defn reset-message-log! []
  (send message-log (constantly empty-message-log)))

;; ============================================================================

(defn- on-player-agent-error [_ ^Throwable ex]
  (log/log ex)
  #_(-> (if (instance? IllegalStateException ex)
          (.getCause ex)
          ex)
        .getMessage
        (or (str ex))
        println))

(defn player-agent [ws]
  (agent {:ws ws}
         :error-handler #'on-player-agent-error))

(defn message->action [_player message]
  (:action message))

(defmulti handle-action! #'message->action)

(defmethod handle-action! :spawn [player message]
  (log/log "spawn:" player)
  (assoc player
         :pos
         (dosync
          (let [pos  (level/random-pos world/world)
                pos' (world/add-object-at! pos (world/make-player))]
            (log-message! (assoc message
                                 :pos    pos'
                                 :object (world/get-object-at pos')))
            pos'))))

(defmethod handle-action! :leave [player message]
  (log/log "leave:" player)
  (let [pos (:pos player)]
    (dosync
     (log-message! (assoc message :pos pos))
     (world/del-object-at! pos)))
  (dissoc player :pos))

(defmethod handle-action! :move [player {dir :dir :as message}]
  (log/log "move:" player dir)
  (update player
          :pos
          #(dosync
            (log-message! (assoc message :pos %))
            (movement/move-object-at! world/world % dir))))

(defn player-action! [player message]
  (log/log "player-action!" player message)
  (send player handle-action! message))

;; ============================================================================

(defn- on-connect [ws]
  (try
    (let [wsk    (ws-key ws)
          _      (log/log "on-connect:" wsk)
          player (player-agent ws)]
      (swap! ws->player assoc wsk player)

      ;; sending directly
      (send-to-player! player
                       {:action :snapshot
                        :world  (world/make-snapshot)})

      ;; TODO: ensure seamless integration of snapshot and the message log
      (player-action! player {:action :spawn}))

    (catch Exception ex
      (log/log ex))))

(defn- on-close [ws _ _]
  (let [wsk    (ws-key ws)
        _      (log/log "on-close:" wsk)
        player (get @ws->player wsk)]
    (swap! ws->player dissoc wsk)

    (if (some? player)
      (player-action! player {:action :leave})
      (log/log "!!! No player found for:" wsk))))

(defn- on-text [ws text]
  (try
    (let [wsk     (ws-key ws)
          _       (log/log "on-text:" wsk)
          player  (get @ws->player wsk)
          message (clojure.edn/read-string text)]
      ;; TODO: check that action is allowed
      (player-action! player message))
    (catch Exception ex
      (log/log ex))))

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
  (when-some [server @web-server]
    (.stop ^Server server)
    (reset! web-server nil)))

(defn start! []
  (world/create-world!)
  (start-web-server!))

(defn stop! []
  (stop-web-server!)
  (world/destroy-world!)
  (reset-message-log!))

(defn restart! []
  (stop!)
  (start!))

(comment
  (restart!)
  message-log
  )
