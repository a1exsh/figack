(ns figack.client.ascii.level.walls
  (:require figack.level.walls
            [figack.client.ascii.level.render :as render]))

(defn which-wall [w]
  (:dir w))

(defmulti render-wall #'which-wall)

(defmethod render-wall :WE [_]
  \-)

(defmethod render-wall :NS [_]
  \|)

(extend-type figack.level.walls.Wall
  render/Render
  (render [this]
    (render-wall this)))
