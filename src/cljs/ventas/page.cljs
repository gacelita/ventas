(ns ventas.page
  (:require [reagent.session :as session]))

(defmulti pages identity)

(defmethod pages :not-found []
  [:span
   [:h1 (i18n ::404)]])

(defmethod pages :default []
  [:span
   [:h1 (i18n ::not-implemented)]
   [:p (i18n ::this-page-has-not-been-implemented)]])