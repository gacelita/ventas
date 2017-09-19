(ns ventas.pages.admin
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [re-frame.core :as rf]
            [bidi.bidi :as bidi]
            [re-frame-datatable.core :as dt]
            [ventas.page :refer [pages]]
            [ventas.routes :as routes]
            [ventas.util :refer [dispatch-page-event]]))

(defn menu []
  [:ul
   [:li [:a {:href (routes/path-for :admin.users)} "Users"]]
   [:li [:a {:href (routes/path-for :admin.products)} "Products"]]])

(defn skeleton [content]
  [:div.admin__skeleton
   [:div.admin__sidebar
    [:a {:href (routes/path-for :admin)}
     [:h3 "Administration"]]
    [menu]]
   [:div.admin__content
    content]])

(defmethod pages :admin []
  [skeleton
   [:p.admin__default-content "Nothing here"]])