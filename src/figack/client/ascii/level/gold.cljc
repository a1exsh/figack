(ns figack.client.ascii.level.gold
  (:require [figack.client.ascii.level.render :as render])
  ;(:import [figack.level.gold Gold])
  )

(extend-type figack.level.gold.Gold
  render/Render
  (render [_]
    \$))
