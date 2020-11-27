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
   [taoensso.sente.packers.transit :as sente-transit]))

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

(defn landing-page-handler [ring-req]
  (hiccup/html
    [:h1 "Sente reference example"]
    (let [csrf-token
          ;; (:anti-forgery-token ring-req) ; Also an option
          (force ring.middleware.anti-forgery/*anti-forgery-token*)]
      [:div#sente-csrf-token {:data-csrf-token csrf-token}])
    [:p "An Ajax/WebSocket" [:strong " (random choice!)"] " has been configured for this example"]
    [:hr]
    [:p [:strong "Step 1: "] " try hitting the buttons:"]
    [:p
     [:button#btn1 {:type "button"} "chsk-send! (w/o reply)"]
     [:button#btn2 {:type "button"} "chsk-send! (with reply)"]]
    [:p
     [:button#btn3 {:type "button"} "Test rapid server>user async pushes"]
     [:button#btn4 {:type "button"} "Toggle server>user async broadcast push loop"]]
    [:p
     [:button#btn5 {:type "button"} "Disconnect"]
     [:button#btn6 {:type "button"} "Reconnect"]]
    ;;
    [:p [:strong "Step 2: "] " observe std-out (for server output) and below (for client output):"]
    [:textarea#output {:style "width: 100%; height: 200px;"}]
    ;;
    [:hr]
    [:h2 "Step 3: try login with a user-id"]
    [:p  "The server can use this id to send events to *you* specifically."]
    [:p
     [:input#input-login {:type :text :placeholder "User-id"}]
     [:button#btn-login {:type "button"} "Secure login!"]]
    ;;
    [:hr]
    [:h2 "Step 4: want to re-randomize Ajax/WebSocket connection type?"]
    [:p "Hit your browser's reload/refresh button"]
    [:script {:src "cljs-out/dev-main.js"}] ; Include our cljs target
    ))

(defn login-handler
  [{:keys [session params]}]
  (println (format "Login request: %s" params))
  {:status  200
   :session (assoc session :uid (:user-id params))})

(defroutes ring-routes
  (GET  "/"      ring-req (landing-page-handler          ring-req))
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

(defn start-example-broadcaster!
  "As an example of server>user async pushes, setup a loop to broadcast an
  event to all connected users every 10 seconds"
  []
  (reset! broadcast-enabled? true)
  (letfn [(broadcast!
            [i]
            (let [uids (:any @connected-uids)]
              (println (format "Broadcasting server>user: %s uids" (count uids)))
              (doseq [uid uids]
                (chsk-send! uid
                            [:some/broadcast
                             {:what-is-this "An async broadcast pushed from server"
                              :how-often "Every 10 seconds"
                              :to-whom uid
                              :i i}]))))]
    (go-loop [i 0]
      (<! (async/timeout 10000))
      (when @broadcast-enabled?
        (broadcast! i)
        (recur (inc i))))))

(defn stop-example-broadcaster!
  []
  (reset! broadcast-enabled? false))

(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  ;;(-event-msg-handler ev-msg) ; Handle event-msgs on a single thread
  ;; (future (-event-msg-handler ev-msg)) ; Handle event-msgs on a thread pool
  (println (format "msg: %s" ev-msg))
  )

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
  (stop-router!)
  (stop-web-server!)
  (stop-example-broadcaster!))

(defn start!
  []
  (start-router!)
  (start-web-server! server-port)
  (start-example-broadcaster!))

(comment
  (start!)
  (stop!))
