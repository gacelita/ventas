(ns ventas.themes.blank.core
  "A blank theme, meant as a starting point for themes"
  (:require
   [ventas.theme :as theme]))

(theme/register!
 :blank
 {:name "Blank"
  :build {:main 'ventas.themes.blank.core}
  :fixtures
  (fn []
    [])
  :migrations []})
