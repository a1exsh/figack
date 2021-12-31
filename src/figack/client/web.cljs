(ns figack.client.web
  (:require-macros [cljs.core.async.macros :as macros])
  (:require cljs.reader
            [cljs.core.async :as async]

            [haslett.client :as ws]

            [figack.field :as field]
            [figack.level :as level]
            [figack.level.beings]
            [figack.level.gold]
            [figack.level.walls]
            [figack.movement :as movement]

            [figack.client.ascii]))

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

;;==============================================================================
(defonce world (atom nil))

(def level-el (.getElementById js/document "level"))

(defn render-level
  [_ _ _ new-state]
  ;; (->output! "new-state: " (str new-state))
  (let [rendered (with-out-str (figack.client.ascii/print-snapshot new-state))]
    #_(->output! rendered)
    (aset level-el "innerHTML" rendered)))

(add-watch world :render-level render-level)

(def keycode->dir
  {38 :N
   40 :S
   37 :W
   39 :E})

(defn make-keydown-handler
  [sink]
  (fn [event]
    (let [keycode (.-keyCode event)]
      (if-some [dir (keycode->dir keycode)]
        (macros/go
          (async/>! sink (pr-str {:action :move
                                  :dir    dir})))))))

(defn- message->action [message]
  (:action message))

(defmulti handle-message! #'message->action)

(defmethod handle-message! :default [message]
  (->output! "unknown message: " (str message)))

(defmethod handle-message! :snapshot [message]
  (->output! "got world snapshot!")
  (reset! world (:world message)))

(defmethod handle-message! :spawn [{:keys [pos object]}]
  (->output! "spawn: " pos object)
  (swap! world
         #(level/update-field-at % pos
                                 field/put-object (:id pos) object)))

(defmethod handle-message! :leave [{:keys [pos]}]
  (->output! "leave: " pos)
  (swap! world
         #(level/update-field-at % pos
                                 field/del-object (:id pos))))

(defmethod handle-message! :move [{:keys [pos dir]}]
  (->output! "move: " pos dir)
  (swap! world
         #(movement/move-object-at % pos dir)))

(defn start!
  []
  (macros/go
    (let [{:keys [source sink]} (async/<! (ws/connect "ws://localhost:8080/websockets/"))]

      (.addEventListener js/document "keydown" (make-keydown-handler sink))

      (loop []
        (when-some [raw-str (async/<! source)]
          ;; (->output! "raw-str: " raw-str)

          (let [message (cljs.reader/read-string raw-str)]
            (handle-message! message))
          ;; (async/<! (async/timeout 1))

          (recur)))

      (->output! "Connection closed!"))))
