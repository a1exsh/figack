(ns figack.validations)

(def ^:private field-validators {})

(defn set-field-validator! [key vfn]
  (alter-var-root #'field-validators assoc key vfn))

(defn validate-field [field]
  (->> field-validators
       vals
       (map #(% field))
       dorun)
  true)
