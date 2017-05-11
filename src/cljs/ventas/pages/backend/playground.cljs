(ns ventas.pages.backend.playground
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [re-frame.core :as rf]
            [bidi.bidi :as bidi]
            [re-frame-datatable.core :as dt]
            [ventas.page :refer [pages]]
            [ventas.util :refer [go-to dispatch-page-event]]))

(defmethod pages :backend.playground []
  (fn page-playground []
    [:div
      [:p "Development playground"]
      [:input {:type "file" :on-change #(rf/dispatch [:app/upload {:file (-> % .-target .-files (aget 0)) :source 17592186045450}])}]
      [:button {:on-click #(rf/dispatch [:app/notifications.add {:message "This a test notification" :theme "warning"}])} "Add a notification"]]))