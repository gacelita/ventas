(ns ventas.events
  (:require [clojure.core.async :refer [<! >! go-loop chan]]))

(def init (chan))
