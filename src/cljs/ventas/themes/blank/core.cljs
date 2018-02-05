(ns ventas.themes.blank.core
  "See the docstring in the server version of this file"
  (:require
   [ventas.core]
   [ventas.i18n :as i18n]
   [ventas.themes.blank.pages.frontend]))

(i18n/register-translations!
 {:en_US
  {}})
