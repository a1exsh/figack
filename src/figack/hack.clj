(ns figack.hack
  (:require [figack.level :as level]
            [figack.level.walls :as walls]
            [figack.client.repl :as repl]))

(defonce world (level/empty-level))

(defn make-snapshot []
  (dosync
   (into [] (map deref world))))

(defn build-border-walls! []
  (let [first-line (level/get-line world 0)
        last-line  (level/get-line world (dec level/height))]
    (dosync
     (doseq [x (range level/width)]
       (ref-set (nth first-line x) walls/WE)
       (ref-set (nth last-line  x) walls/WE))

     (doseq [y (range 1 (dec level/height))
             :let [line (level/get-line world y)]]
       (ref-set (nth line 0) walls/NS)
       (ref-set (nth line (dec level/width)) walls/NS)))))

;; player is not a field!
(defn make-player []
  {:class :human})

(comment
  (build-border-walls!)
  (dosync
   (alter (nth (level/get-line world 3) 4)
          #(assoc % :being (make-player))))

  @(nth (level/get-line world 3) 4)

  (->> (make-snapshot) repl/render-snapshot)
;; ----------------------------------------
;; |......................................|
;; |......................................|
;; |...@..................................|
;; |......................................|
;; |......................................|
;; |......................................|
;; |......................................|
;; |......................................|
;; ----------------------------------------
  )
