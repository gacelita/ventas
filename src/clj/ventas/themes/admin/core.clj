(ns ventas.themes.admin.core
  (:require [ventas.theme :as theme]
            [clojure.string :as str]))

(theme/register!
 :admin
 {:build {:modules {:main {:entries ['ventas.themes.admin.core]}}}
  :should-load? (fn [{:keys [uri]}]
                  (str/starts-with? uri "/admin"))})
