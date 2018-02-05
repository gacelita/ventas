(ns ventas.components.image
  (:require
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [ventas.events :as events]
   [ventas.components.base :as base]))

(def state-key ::state)

(defn image [id size]
  {:pre [(keyword? size)]}
  (rf/dispatch [::events/image-sizes.list])
  (fn [id size]
    (let [loaded? @(rf/subscribe [::events/db [state-key [id size]]])]
      (when-let [{:keys [width height]} @(rf/subscribe [::events/db [:image-sizes size]])]
        [:div.image-component {:style {:width (dec width)
                                       :height height}}
         (when-not loaded?
           [:div.image-component__dimmer
            [base/loading]])
         [:div.image-component__inner
          [:img {:style (when-not loaded? {:display "none"})
                 :on-load #(rf/dispatch [::events/db [state-key [id size]] true])
                 :src (str "images/" id "/resize/" (name size))}]]]))))
