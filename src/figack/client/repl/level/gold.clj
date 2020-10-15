(ns figack.client.repl.level.gold
  (:require [figack.client.repl.level.render :refer [Render]])
  (:import [figack.level.gold Gold]))

(extend-type Gold
  Render
  (render [_]
    \$))
