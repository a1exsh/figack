(ns figack.client.ascii
  (:require clojure.string
            [figack.level :as level]
            [figack.client.ascii.level.render :as render]
            ;; we only require these so that render protocol is extended:
            figack.client.ascii.level
            figack.client.ascii.level.beings
            figack.client.ascii.level.gold
            figack.client.ascii.level.walls))

(defn render-line [line]
  (clojure.string/join (map render/to-char line)))

(defn print-snapshot [level]
  (doseq [y (range (:height level))]
    (println (render-line (level/get-line level y)))))
