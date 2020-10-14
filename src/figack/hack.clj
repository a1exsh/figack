(ns figack.hack
  (:require [figack.level :as level]
            [figack.level
             [beings :as beings]
             [walls :as walls]]
            [figack.client.repl :as repl]))

(def #_once world (level/empty-level))

(defn make-snapshot []
  (dosync
   (into [] (map deref world))))

#_(defn find-in [field obj-type]
  (->> field
       :objects
       (filter #(-> % :type (= obj-type)))))

(defn validate-field [field]
  (when (-> field :objects count (> 1))
    (throw (Exception. "Too many objects on one field.")))

  #_(when (and (instance? figack.level.walls.Wall field)
                 (not (empty? beings)))
        (throw (Exception. "It bumps into a wall.")))

  true)

(defn set-validations! []
  (->> world
       (map #(set-validator! % validate-field))
       dorun))

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
  (beings/map->Being {:class :human}))

(defonce object-id (atom 0))

(defn next-object-id []
  (swap! object-id inc))

(defn add-object-at!
  "Adds a newly created object `obj` to the field at position `pos` and
  returns the position complete with object id."
  [pos obj]
  (let [obj-id (next-object-id)]
    (alter (level/get-field-at world pos)
           assoc-in
           [:objects obj-id]
           obj)
    (assoc pos :id obj-id)))

(defonce player-pos (atom nil))

(defn create-world! []
  (set-validations!)
  (build-border-walls!)
  (reset! player-pos
          (dosync
           (add-object-at! {:x 4 :y 3}
                           (make-player)))))

(defn print-help []
  (println "h=help q=quit"))

(def move-dirs #{:N :NE :E :SE :S :SW :W :NW})

(defn new-pos-for-move
  "Calculates the new virtual position for a given move direction.  Performs no
  boundary checks."
  [{:keys [x y]} dir]
  {:pre [(contains? move-dirs dir)]}
  (case dir
    :N  {:x      x  :y (dec y)}
    :NE {:x (inc x) :y (dec y)}
    :E  {:x (inc x) :y      y}
    :SE {:x (inc x) :y (inc y)}
    :S  {:x      x  :y (inc y)}
    :SW {:x (dec x) :y (inc y)}
    :W  {:x (dec x) :y      y}
    :NW {:x (dec x) :y (dec y)}))

(defn report-exception! [^Exception ex]
  (-> (if (instance? java.lang.IllegalStateException ex)
        (.getCause ex)
        ex)
      .getMessage
      println))

(defn move-object-at!
  "Tries to move an object that should be found in the world at position `pos`
  in the direction given by `dir` and returns the new position (which might be
  unchanged, in case of hitting an obstacle, or different from the expected
  position, in case of other interactions)."
  [pos dir]
  (try
    (let [new-pos (new-pos-for-move pos dir)
          src (level/get-field-at world pos)
          dst (level/get-field-at world new-pos)
          obj-id (:id pos)]
      (dosync
       (let [obj (get-in @src [:objects obj-id])]
         (assert obj (str "The object must be found at the given position:" pos))
         (alter src update :objects dissoc obj-id)
         (alter dst assoc-in [:objects obj-id] obj)))
      (assoc new-pos :id obj-id))
    (catch Exception ex
      (report-exception! ex)
      pos)))

(defn move-player! [dir]
  (swap! player-pos move-object-at! dir))

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
    (repl/print-snapshot (make-snapshot))
    (when-not (= ::quit (read-and-act!))
      (recur))))

(comment
  (create-world!)
  (play!)
  )
