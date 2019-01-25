(ns ventas.themes.dev.core
  (:require
   [ventas.theme :as theme]))

(theme/register!
 :dev
 {:build {:modules {:main {:entries ['ventas.themes.dev.core]}}}
  :default? true})
