(ns figack.client.ascii.level.gold
  (:require figack.level.gold
            [figack.client.ascii.level.render :as render]))

(extend-type figack.level.gold.Gold
  render/Render
  (to-char [_]
    \$))
