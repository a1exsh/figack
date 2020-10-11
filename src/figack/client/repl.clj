(ns figack.client.repl
  (:require clojure.string
            [figack.level :as level]
            [figack.client.repl.level :as lev]))

(defn render-line [line]
  (clojure.string/join (map lev/render-field line)))

(defn render-snapshot [fields]
  (println)
  (doseq [y (range level/height)]
    (println (render-line (level/get-line fields y)))))
