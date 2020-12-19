(ns figack.client.repl.level.gold
  (:require [figack.client.repl.level.render :as render])
  ;(:import [figack.level.gold Gold])
  )

(extend-type figack.level.gold.Gold
  render/Render
  (render [_]
    \$))
