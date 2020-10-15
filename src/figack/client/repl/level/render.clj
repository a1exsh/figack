(ns figack.client.repl.level.render)

(defprotocol Render
  (render [this]))

(defn has-renderer? [x]
  (->> x type (extends? Render)))
