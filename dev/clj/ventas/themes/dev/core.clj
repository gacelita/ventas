(ns ventas.themes.dev.core
  (:require
   [ventas.theme :as theme]))

(theme/register!
 :dev
 {:name "Dev"
  :build {:modules {:main {:entries ['ventas.themes.dev.core]}}}
  :fixtures
  (fn []
    [])
  :migrations []})
