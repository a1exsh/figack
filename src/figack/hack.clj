(ns figack.hack
  (:require [figack.server.world :refer [create-world! move-player!] :as world]
            [figack.client.ascii :as ascii]))

(defn print-help []
  (println "h=help q=quit"))

(defn read-and-act! []
  (case (read)
    n  (move-player! :N)
    ne (move-player! :NE)
    e  (move-player! :E)
    se (move-player! :SE)
    s  (move-player! :S)
    sw (move-player! :SW)
    w  (move-player! :W)
    nw (move-player! :NW)
    h  (print-help)
    q  ::quit
    (print-help)))

(defn play! []
  (loop []
    (println)
    (ascii/print-snapshot (world/make-snapshot))
    (when-not (= ::quit (read-and-act!))
      (recur))))

(comment
  (load "figack/hack")
  (in-ns 'figack.hack)

  (create-world!)
  (play!)
  )
