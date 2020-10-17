(ns figack.level.gold)

(defrecord Gold [amount])

(defn make-gold [amount]
  {:pre [(pos? amount)]}
  (->Gold amount))
