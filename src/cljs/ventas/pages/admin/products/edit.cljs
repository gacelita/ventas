(ns ventas.pages.admin.products.edit
  (:require
   [reagent.core :as reagent :refer [atom]]
   [reagent.session :as session]
   [re-frame.core :as rf]
   [bidi.bidi :as bidi]
   [re-frame-datatable.core :as dt]
   [soda-ash.core :as sa]
   [ventas.utils.logging :refer [trace debug info warn error]]
   [ventas.components.base :as base]
   [ventas.page :refer [pages]]
   [ventas.pages.admin.users :as users-page]
   [ventas.routes :as routes]
   [ventas.util :as util :refer [dispatch-page-event]]
   [ventas.utils.ui :as utils.ui]
   [ventas.pages.admin :as admin]
   [ventas.i18n :refer [i18n]]))

(defn page []
  [:h1 "Hello world!"])

(routes/define-route!
 :admin.products.edit
 {:name (i18n ::page)
  :url [:id "/edit"]
  :component page})