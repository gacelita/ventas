(ns ventas.pretty
  (:require [io.aviso.ansi :as clansi]))


(defn print-info [str]
  (println (clansi/green str)))