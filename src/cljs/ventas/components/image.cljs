(ns ventas.components.image
  (:require
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [ventas.events :as events]
   [ventas.components.base :as base]))

(defn image [id size]
  {:pre [id (keyword? size)]}
  (rf/dispatch [::events/image-sizes.list])
  (let [loaded? (reagent/atom false)]
    (fn [id size]
      (when-let [{:keys [width height]} @(rf/subscribe [::events/db [:image-sizes size]])]
        [:div.image-component {:style {:width width
                                       :height height}}
         [:div.image-component__dimmer (when @loaded? {:style {:display "none"}})
          [base/loading]]
         [:div.image-component__inner
          [:img {:style (when-not @loaded? {:display "none"})
                 :on-load #(reset! loaded? true)
                 :src (str "images/" id "/resize/" (name size))}]]]))))