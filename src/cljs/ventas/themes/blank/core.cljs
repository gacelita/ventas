(ns ventas.themes.blank.core
  "See the docstring in the server version of this file"
  (:require
   [ventas.themes.blank.pages.frontend]
   [ventas.i18n :as i18n]
   [ventas.core]))

(i18n/register-translations!
 {:en_US
  {}})
