(ns ventas.utils.ui)

(defn with-handler [cb]
  (fn [e]
    (doto e
      .preventDefault
      .stopPropagation)
    (cb e)))