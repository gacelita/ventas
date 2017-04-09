(ns ventas.pages.interface
  (:require [reagent.session :as session]))

(defmulti pages identity)

(defmethod pages :not-found []
  "Non-existing routes go here"
  (fn page-not-found []
    [:span
     [:h1 "404: Aquí no hay nada"]
     ]))

(defmethod pages :default []
  "Configured routes, missing an implementation, go here"
  (fn page-default []
    [:span
      [:h1 "No implementado"]
      [:p (str "Esta página ( " (:current-page (session/get :route)) " ) no ha sido implementada.")]]))