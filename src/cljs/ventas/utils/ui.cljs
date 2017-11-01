(ns ventas.utils.ui
  (:require [re-frame.core :as rf]
            [re-frame.registrar :as rf.registrar]))

(defn with-handler [cb]
  (fn [e]
    (doto e
      .preventDefault
      .stopPropagation)
    (cb e)))
