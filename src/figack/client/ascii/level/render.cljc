(ns figack.client.ascii.level.render)

(defprotocol Render
  (to-char [this]))

(defn has-renderer? [x]
  (satisfies? Render x))
