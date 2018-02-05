(ns ventas.themes.clothing.pages.frontend
  (:require
   [re-frame.core :as rf]
   [reagent.core :as reagent :refer [atom]]
   [ventas.components.base :as base]
   [ventas.components.cart :as ventas.cart]
   [ventas.components.category-list :refer [category-list]]
   [ventas.components.notificator :as ventas.notificator]
   [ventas.components.popup :as ventas.popup]
   [ventas.i18n :refer [i18n]]
   [ventas.page :refer [pages]]
   [ventas.plugins.featured-categories.core :as plugins.featured-categories]
   [ventas.plugins.featured-products.core :as plugins.featured-products]
   [ventas.plugins.slider.core :as plugins.slider]
   [ventas.routes :as routes]
   [ventas.themes.clothing.components.header :refer [header]]
   [ventas.themes.clothing.components.heading :as theme.heading]
   [ventas.themes.clothing.components.preheader :refer [preheader]]
   [ventas.themes.clothing.components.skeleton :refer [skeleton]]
   [ventas.themes.clothing.pages.frontend.cart]
   [ventas.themes.clothing.pages.frontend.category]
   [ventas.themes.clothing.pages.frontend.checkout]
   [ventas.themes.clothing.pages.frontend.login]
   [ventas.themes.clothing.pages.frontend.privacy-policy]
   [ventas.themes.clothing.pages.frontend.product]
   [ventas.themes.clothing.pages.frontend.profile]
   [ventas.utils :as util]
   [ventas.utils.logging :refer [debug error info trace warn]]))

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
