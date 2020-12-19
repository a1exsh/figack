(ns figack.level.gold
  #?(:cljs (:require cljs.reader)))

(defrecord Gold [amount])
#?(:cljs (cljs.reader/register-tag-parser! 'figack.level.gold.Gold map->Gold))

(defn make-gold [amount]
  {:pre [(pos? amount)]}
  (->Gold amount))
