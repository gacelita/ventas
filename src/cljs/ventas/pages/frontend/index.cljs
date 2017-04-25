(ns ventas.pages.frontend.index
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [re-frame.core :as rf]
            [bidi.bidi :as bidi]
            [re-frame-datatable.core :as dt]
            [ventas.pages.interface :refer [pages]]
            [ventas.pages.frontend :as frontend]))

(defmethod pages :frontend.index []
  [frontend/skeleton
    [:h2 "Test frontend index"]])