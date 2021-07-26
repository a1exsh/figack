(ns figack.client.web
  (:require-macros [cljs.core.async.macros :as macros])
  (:require cljs.reader
            [cljs.core.async :as async]

            [haslett.client :as ws]

            [figack.field]
            [figack.level]
            [figack.level.beings]
            [figack.level.gold]
            [figack.level.walls]

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
  ;;(->output! (str new-state))
  (let [rendered (with-out-str (figack.client.ascii/print-snapshot new-state))]
    #_(->output! rendered)
    (aset level-el "innerHTML" rendered)))

(add-watch world :render-level render-level)

(defn make-keydown-handler
  [sink]
  (fn [event]
    (let [keycode (.-keyCode event)]
      (if (and (>= keycode 37)
               (<= keycode 40))
        (macros/go
          (async/>! sink (pr-str {:keycode keycode})))))))

(defn start!
  []
  (macros/go
    (let [{:keys [source sink]} (async/<! (ws/connect "ws://localhost:8080/websockets/"))]

      (.addEventListener js/document "keydown" (make-keydown-handler sink))

      (while true
        (reset! world (:world (cljs.reader/read-string (async/<! source))))
        (async/<! (async/timeout 1))))))
