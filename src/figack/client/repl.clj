(ns figack.client.repl
  (:require clojure.string
            [figack.level :as level]
            [figack.client.repl.level.render :as render]
            ;; we only require these so that render protocol is extended:
            figack.client.repl.level
            figack.client.repl.level.walls))

(defn render-line [line]
  (clojure.string/join (map render/render line)))

(defn render-snapshot [fields]
  (println)
  (doseq [y (range level/height)]
    (println (render-line (level/get-line fields y)))))
