(ns ventas.themes.devcards.core
  (:require
   [ventas.theme :as theme]
   [clojure.string :as str]))

(theme/register!
 :devcards
 {:build {:modules {:main {:entries ['ventas.devcards.core]}}
          :devtools {:after-load 'cards.card-ui/refresh
                     :watch-dir "resources/public"}}
  :init-script "devcards.core.start_devcard_ui_BANG__STAR_();"
  :should-load? (fn [{:keys [uri]}]
                  (str/starts-with? uri "/devcards"))})
