(ns ventas.components.image
  (:require
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.events :as events]))

(def state-key ::state)

(defn get-url [id & [size]]
  (if size
    (str "images/" id "/resize/" (name size))
    (str "images/" id)))

(defn image [id size]
  {:pre [(keyword? size)]}
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
               :src (get-url id size)}]]])))
