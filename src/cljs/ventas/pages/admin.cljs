(ns ventas.pages.admin
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [re-frame.core :as rf]
            [bidi.bidi :as bidi]
            [re-frame-datatable.core :as dt]
            [ventas.page :refer [pages]]
            [ventas.routes :refer [go-to] :as routes]
            [ventas.util :refer [dispatch-page-event]]))

(defmethod pages :admin []
  (fn page-app []
    [:div
      [:span
        [:h4 "Routing example: Index"]
        [:ul
          [:li [:a {:href (routes/path-for :admin.users)} "Users"]]
          [:li [:a {:href (routes/path-for :admin.login)} "Login"]]
          [:li [:a {:href (routes/path-for :admin.register)} "Register"]]
          [:li [:a {:href "/borken/link" } "Borken link"]]]]]))