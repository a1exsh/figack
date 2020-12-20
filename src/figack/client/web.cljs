(ns figack.client.web
  (:require
   clojure.string
   cljs.reader
   [cljs.core.async :as async :refer (<! >! put! chan)]
   [taoensso.sente  :as sente :refer (cb-success?)]
   ;;[taoensso.sente.packers.transit :as sente-transit]

   [figack.field]
   [figack.level]
   [figack.level.beings]
   [figack.level.gold]
   [figack.level.walls]

   [figack.client.repl]
   )
  (:require-macros
   [cljs.core.async.macros :as asyncm :refer (go go-loop)]))

;; (timbre/set-level! :trace) ; Uncomment for more logging

;;;; Util for logging output to on-screen console

(def output-el (.getElementById js/document "output"))
(defn ->output! [fmt & args]
  (let [msg (str fmt args) ;; TODO: (apply encore/format fmt args)
        ]
    ;;(timbre/debug msg)
    (println msg)
    ;;(aset output-el "value" (str "• " (.-value output-el) "\n" msg))
    ;;(aset output-el "scrollTop" (.-scrollHeight output-el))
    ))

(->output! "ClojureScript appears to have loaded correctly.")

;;;; Define our Sente channel socket (chsk) client

(def ?csrf-token
  (when-let [el (.getElementById js/document "sente-csrf-token")]
    (.getAttribute el "data-csrf-token")))

(if ?csrf-token
  (->output! "CSRF token detected in HTML, great!")
  (->output! "CSRF token NOT detected in HTML, default Sente config will reject requests"))

(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket-client! "/chsk" ; Must match server Ring routing URL
                                         ?csrf-token
                                         {:type   :auto
                                          :packer :edn})]

  (def chsk       chsk)
  (def ch-chsk    ch-recv)              ; ChannelSocket's receive channel
  (def chsk-send! send-fn)              ; ChannelSocket's send API fn
  (def chsk-state state)                ; Watchable, read-only atom
  )

;;;; Sente event handlers

(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  :id ; Dispatch on event-id
  )

(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (-event-msg-handler ev-msg))

(defmethod -event-msg-handler
  :default ; Default/fallback case (no other matching handler)
  [{:as ev-msg :keys [event]}]
  (->output! "Unhandled event: %s" event))

(defmethod -event-msg-handler :chsk/state
  [{:as ev-msg :keys [?data]}]
  (->output! "state change: %s" ?data)
  (let [[old-state-map new-state-map] ?data
        ;; TODO: (have vector? ?data)
        ]
    (if (:first-open? new-state-map)
      (->output! "Channel socket successfully established!: %s" new-state-map)
      (->output! "Channel socket state change: %s"              new-state-map))))

(defmulti msg-handler first)

(defmethod msg-handler :default
  [[kind _]]
  (->output! "Unhandled msg: %s" kind))

(defmethod -event-msg-handler :chsk/recv
  [{:as ev-msg :keys [?data]}]
  (->output! "Push event from server: %s" (first ?data))
  (msg-handler ?data))

;;==============================================================================
(defonce world (atom nil))

(defmethod msg-handler :figack.server.core/snapshot
  [[_ {:keys [snapshot]}]]
  (reset! world snapshot))

(def level-el (.getElementById js/document "level"))

(defn render-level
  [_ _ _ new-state]
  (let [rendered (with-out-str (figack.client.repl/print-snapshot new-state))]
    #_(->output! rendered)
    (aset level-el "innerHTML" rendered)))

(add-watch world :render-level render-level)

(defn take-world-snapshot!
  [timeout-in-ms]
  (chsk-send!
   [:figack.server.core/snapshot]
   timeout-in-ms
   (fn [cb-reply]
     ;;(->output! "Snapshot reply: %s" cb-reply)
     (reset! world (:snapshot cb-reply)))))

(defmethod -event-msg-handler :chsk/handshake
  [{:as ev-msg :keys [?data]}]
  (let [[?uid ?csrf-token ?handshake-data] ?data]
    (->output! "Handshake: %s" ?data)
    (take-world-snapshot! 1000)))

;;==============================================================================
(defmethod -event-msg-handler :figack.server.core/update
  [{:as ev-msg :keys [?data]}]
  )

;;;; Sente event router (our `event-msg-handler` loop)

(defonce router_ (atom nil))
(defn  stop-router! [] (when-let [stop-f @router_] (stop-f)))
(defn start-router! []
  (stop-router!)
  (reset! router_
    (sente/start-client-chsk-router!
      ch-chsk event-msg-handler)))

;;;; Init stuff

(comment
  (defn login
    [user-id]
    (do
      (->output! "Logging in with user-id %s" user-id)
      (sente/ajax-lite "/login"
                       {:method :post
                        :headers {:X-CSRF-Token (:csrf-token @chsk-state)}
                        :params  {:user-id (str user-id)}}

                       (fn [{:keys [success?] :as ajax-resp}]
                         (->output! "Ajax login response: %s" ajax-resp)
                         (if-not success?
                           (->output! "Login failed")
                           (do
                             (->output! "Login successful")
                             (sente/chsk-reconnect! chsk)))))))


  (when-let [target-el (.getElementById js/document "login")]
    (.addEventListener target-el "click"
                       (fn [ev]
                         (->output! "Login!")
                         (login "ash"))))

  (when-let [target-el (.getElementById js/document "start")]
    (.addEventListener target-el "click"
                       (fn [ev]
                         (->output! "Start!")
                         (take-world-snapshot! 5000)))))

(defn start!
  []
  (start-router!))
