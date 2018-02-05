(ns ventas.themes.clothing.pages.frontend
  (:require
   [reagent.core :as reagent :refer [atom]]
   [re-frame.core :as rf]
   [ventas.utils.logging :refer [trace debug info warn error]]
   [ventas.page :refer [pages]]
   [ventas.components.notificator :as ventas.notificator]
   [ventas.components.popup :as ventas.popup]
   [ventas.components.category-list :refer [category-list]]
   [ventas.components.cart :as ventas.cart]
   [ventas.plugins.slider.core :as plugins.slider]
   [ventas.plugins.featured-products.core :as plugins.featured-products]
   [ventas.plugins.featured-categories.core :as plugins.featured-categories]
   [ventas.themes.clothing.components.header :refer [header]]
   [ventas.themes.clothing.components.skeleton :refer [skeleton]]
   [ventas.themes.clothing.components.preheader :refer [preheader]]
   [ventas.themes.clothing.components.heading :as theme.heading]
   [ventas.themes.clothing.pages.frontend.category]
   [ventas.themes.clothing.pages.frontend.product]
   [ventas.themes.clothing.pages.frontend.privacy-policy]
   [ventas.themes.clothing.pages.frontend.login]
   [ventas.themes.clothing.pages.frontend.cart]
   [ventas.themes.clothing.pages.frontend.checkout]
   [ventas.themes.clothing.pages.frontend.profile]
   [ventas.utils :as util]
   [ventas.components.base :as base]
   [ventas.routes :as routes]
   [ventas.i18n :refer [i18n]]))

(defn page []
  [skeleton
   [:div
    [plugins.slider/slider :sample-slider]
    [base/container
     [category-list]
     [theme.heading/heading (i18n ::suggestions-of-the-week)]
     [plugins.featured-products/featured-products]
     [theme.heading/heading (i18n ::recently-added)]
     [plugins.featured-categories/featured-categories]]]])

(routes/define-route!
  :frontend
  {:name ::page
   :url ""
   :component page})
