(ns figack.client.ascii.level.render)

(defprotocol Render
  (render [this]))

(defn has-renderer? [x]
  (satisfies? Render x))
