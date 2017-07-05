(ns ventas.pages.backend
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [re-frame.core :as rf]
            [bidi.bidi :as bidi]
            [re-frame-datatable.core :as dt]
            [ventas.page :refer [pages]]
            [ventas.routes :refer [go-to] :as routes]
            [ventas.util :refer [dispatch-page-event]]))

(defmethod pages :backend []
  (fn page-app []
    [:div
      [:span
        [:h4 "Routing example: Index"]
        [:ul
          [:li [:a {:href (routes/path-for :backend.section-a)} "Section A"]]
          [:li [:a {:href (routes/path-for :backend.users)} "Users"]]
          [:li [:a {:href (routes/path-for :backend.session-debug)} "Session debug"]]
          [:li [:a {:href (routes/path-for :backend.login)} "Login"]]
          [:li [:a {:href (routes/path-for :backend.register)} "Register"]]
          [:li [:a {:href (routes/path-for :frontend.index)} "Frontend index"]]
          [:li [:a {:href "/borken/link" } "Borken link"]]]]]))