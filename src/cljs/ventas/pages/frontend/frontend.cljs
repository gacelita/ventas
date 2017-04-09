(ns ventas.pages.frontend
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [re-frame.core :as rf]
            [bidi.bidi :as bidi]
            [re-frame-datatable.core :as dt]
            [ventas.pages.interface :refer [pages]]
            [ventas.routes :refer [app-routes]]
            [ventas.util :refer [go-to dispatch-page-event]]))

(defmethod pages :frontend []
  (fn page-app []
    [:div
      [:span
        [:h4 "Routing example: Index"]
        [:ul
          [:li [:a {:href (bidi/path-for app-routes :backend.section-a) } "Section A"]]
          [:li [:a {:href (bidi/path-for app-routes :backend.section-b) } "Section B"]]
          [:li [:a {:href (bidi/path-for app-routes :backend.missing-route) } "Missing-route"]]
          [:li [:a {:href (bidi/path-for app-routes :backend.users)} "Users"]]
          [:li [:a {:href (bidi/path-for app-routes :backend.session-debug)} "Session debug"]]
          [:li [:a {:href (bidi/path-for app-routes :backend.login)} "Login"]]
          [:li [:a {:href (bidi/path-for app-routes :backend.register)} "Register"]]
          [:li [:a {:href (bidi/path-for app-routes :frontend.index)} "Frontend index"]]
          [:li [:a {:href "/borken/link" } "Borken link"]]]]]))