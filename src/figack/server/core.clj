;; https://github.com/ptaoussanis/sente/blob/master/example-project/src/example/server.clj
(ns figack.server.core
  (:require
   [clojure.core.async :as async  :refer (<! <!! >! >!! put! chan go go-loop)]
   [ring.middleware [defaults] [anti-forgery]]
   [ring.util.response :refer [resource-response content-type]]
   [compojure.core :refer (defroutes GET POST)]
   [compojure.route :as route]
   [hiccup.core :as hiccup]
   [org.httpkit.server :as http-kit]
   [taoensso.sente :as sente]
   [taoensso.sente.server-adapters.http-kit :refer (get-sch-adapter)]
   [taoensso.sente.packers.transit :as sente-transit]
   ;;
   [figack.server.world :as world]))

(let [{:keys [ch-recv send-fn connected-uids
              ajax-post-fn ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket-server! (get-sch-adapter) {:packer :edn})
      ;; (sente-transit/get-transit-packer) ; Needs Transit dep
      ]

  (def ring-ajax-post                ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                       ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!                    send-fn) ; ChannelSocket's send API fn
  (def connected-uids                connected-uids) ; Watchable, read-only atom
  )

(add-watch connected-uids :connected-uids
           (fn [_ _ old new]
             (when (not= old new)
               (println (format "Connected uids change: %s" new)))))

(defn landing-page-handler
  [ring-req]
  (hiccup/html
   [:div#sente-csrf-token
    {:data-csrf-token (force ring.middleware.anti-forgery/*anti-forgery-token*)}
    [:textarea#level {:readonly "true" :rows 25 :cols 80}]
    ;;[:button#login {:type "button"} "Login"]
    ;;[:button#start {:type "button"} "Start!"]
    ]
   [:script {:src "cljs-out/dev-main.js"}] ; Include our cljs target
   ))

(defn login-handler
  [{:keys [session params]}]
  (println (format "Login request: %s" params))
  {:status  200
   :session (assoc session :uid (:user-id params))})

(defroutes ring-routes
  (GET  "/"      ring-req (#'landing-page-handler        ring-req))
  (GET  "/chsk"  ring-req (ring-ajax-get-or-ws-handshake ring-req))
  (POST "/chsk"  ring-req (ring-ajax-post                ring-req))
  (POST "/login" ring-req (login-handler                 ring-req))
  (route/resources "/")
  (route/not-found "<h1>Page not found</h1>\n"))

(def main-ring-handler
  "**NB**: Sente requires the Ring `wrap-params` + `wrap-keyword-params`
  middleware to work. These are included with
  `ring.middleware.defaults/wrap-defaults` - but you'll need to ensure
  that they're included yourself if you're not using `wrap-defaults`.
  You're also STRONGLY recommended to use `ring.middleware.anti-forgery`
  or something similar."
  (ring.middleware.defaults/wrap-defaults
   ring-routes
   ring.middleware.defaults/site-defaults))

(defonce broadcast-enabled? (atom nil))

(defn broadcast-snapshot!
  [snapshot]
  (let [uids (:any @connected-uids)]
    (when (-> uids count (> 0))
      #_(println (format "Broadcasting server>user: %s uids" (count uids)))
      (doseq [uid uids]
        (chsk-send! uid [::snapshot {:snapshot snapshot}])))))

(defn start-example-broadcaster!
  "As an example of server>user async pushes, setup a loop to broadcast an
  event to all connected users every 10 seconds"
  []
  (reset! broadcast-enabled? true)
  (go-loop [i 0]
    (<! (async/timeout 1000))
    (when @broadcast-enabled?
      (broadcast-snapshot! (world/make-snapshot))
      (recur (inc i)))))

(defn stop-example-broadcaster!
  []
  (reset! broadcast-enabled? false))

(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  :id ; Dispatch on event-id
  )

(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (println (format "MSG: %s" ev-msg))

  (-event-msg-handler ev-msg) ; Handle event-msgs on a single thread
  ;; (future (-event-msg-handler ev-msg)) ; Handle event-msgs on a thread pool
  )

(defmethod -event-msg-handler
  :default ; Default/fallback case (no other matching handler)
  [{:as ev-msg :keys [event]}]
  (println (format "Unhandled event: %s" event)))

;;==============================================================================
(defmethod -event-msg-handler ::snapshot
  [{:as ev-msg :keys [?reply-fn]}]
  (let [snapshot (world/make-snapshot)]
    ;;(println (format "Sending snapshot: %s" snapshot))
    (?reply-fn {:snapshot snapshot})))

(defonce router (atom nil))

(defn stop-router!
  []
  (when-let [stop-fn @router]
    (stop-fn)))

(defn start-router!
  []
  (stop-router!)
  (reset! router (sente/start-server-chsk-router! ch-chsk event-msg-handler)))

(defonce web-server (atom nil))

(defn stop-web-server!
  []
  (when-let [stop-fn @web-server]
    (stop-fn)))

(defn start-web-server!
  [& [port]]
  (stop-web-server!)

  (let [stop-fn (http-kit/run-server #'main-ring-handler {:port port})]
    (println (format "Web server is running at `http://localhost:%s`" port))
    (reset! web-server
            (fn []
              (println "Stopping web server...")
              (stop-fn :timeout 100)))))

(def server-port 8080)

(defn stop!
  []
  (stop-example-broadcaster!)
  (stop-web-server!)
  (stop-router!)
  (world/destroy-world!))

(defn start!
  []
  (world/create-world!)
  (start-router!)
  (start-web-server! server-port)
  (start-example-broadcaster!))

(comment
  (start!)
  (stop!)

  (start-example-broadcaster!)
  (stop-example-broadcaster!))
