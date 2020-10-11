(ns figack.hack
  (:require clojure.string))

(def width 40)
(def height 10)

(defn empty-field []
  {})

(def level (into [] (repeatedly (* width height)
                                #(ref (empty-field)))))

(defn make-snapshot []
  (dosync
   (into [] (map deref level))))

(defn which-field [field]
  (:type field))

(defmulti render-field #'which-field)

(defmethod render-field :default [_]
  \?)

(defn render-line [line]
  (clojure.string/join (map render-field line)))

(defn get-line [fields y]
  (subvec fields
          (* y width)
          (* (inc y) width)))

(defn render-snapshot [fields]
  (println)
  (doseq [y (range height)]
    (println (render-line (get-line fields y)))))

(defn make-wall-field [dir]
  {:pre [(contains? #{:we :ns} dir)]}
  {:type :wall
   :dir  dir})

(defn which-wall [w]
  (:dir w))

(defmulti render-wall #'which-wall)

(defmethod render-wall :we [_]
  \-)

(defmethod render-wall :ns [_]
  \|)

(defmethod render-field :wall [w]
  (render-wall w))

(defn build-border-walls []
  (let [we-wall (make-wall-field :we)
        ns-wall (make-wall-field :ns)
        first-line (get-line level 0)
        last-line  (get-line level (dec height))]
    (dosync
     (doseq [x (range width)]
       (ref-set (nth first-line x) we-wall)
       (ref-set (nth last-line  x) we-wall))

     (doseq [y (range 1 (dec height))
             :let [line (get-line level y)]]
       (ref-set (nth line 0) ns-wall)
       (ref-set (nth line (dec width)) ns-wall)))))

;; player is not a field!
(defn make-player []
  {:class :human})

(defn which-being [being]
  (:class being))

(defmulti render-being #'which-being)

(defmethod render-being :human [_]
  \@)

(defmethod render-field nil [field]
  (if-let [being (:being field)]
    (render-being being)
    \.))

(comment
  (build-border-walls)
  (dosync
   (alter (nth (get-line level 3) 4) #(assoc % :being (make-player))))

  @(nth (get-line level 3) 4)

  (->> (make-snapshot) render-snapshot)
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
