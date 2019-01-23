(ns ventas.themes.devcards.core
  (:require
   [ventas.theme :as theme]))

(theme/register!
 :devcards
 {:name "Devcards"
  :build {:modules {:main {:entries ['ventas.devcards.core]}}
          :devtools {:after-load 'cards.card-ui/refresh
                     :watch-dir "resources/public"}}
  :fixtures
  (fn []
    [])
  :migrations []})
