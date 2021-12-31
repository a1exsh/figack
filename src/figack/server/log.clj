(ns figack.server.log)

(defn log [& args]
  (io!
   (locking 'log
     (apply println args))))
