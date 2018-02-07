(ns ventas.page
  "Just the page multimethod"
  (:require
   [ventas.i18n :refer [i18n]]))

(defmulti pages identity)

(defmethod pages :not-found []
  [:span
   [:h1 (i18n ::not-found)]])

(defmethod pages :default []
  [:span
   [:h1 (i18n ::not-implemented)]
   [:p (i18n ::this-page-has-not-been-implemented)]])

(defn main [handler]
  [:div#main
   [pages handler]])