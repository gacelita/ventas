(ns ventas.components.image
  (:require
   [re-frame.core :as rf]
   [ventas.components.base :as base]))

(def state-key ::state)

(defn get-url [id & [size]]
  (if size
    (str "images/" id "/resize/" (name size))
    (str "images/" id)))

(defn image [id size]
  {:pre [(keyword? size)]}
  (let [status @(rf/subscribe [:db [state-key [id size]]])]
    (when-let [{:keys [width height]} @(rf/subscribe [:db [:image-sizes size]])]
      [:div.image-component {:style {:width (dec width)
                                     :height height}}
       (when-not status
         [:div.image-component__dimmer
          [base/loading]])
       (when (= :status/error status)
         [:div.image-component__error
          [:div
           [base/icon {:name "image"}]
           [:p "Error loading this image"]]])
       [:div.image-component__inner
        [:img {:style (when-not (= status :status/loaded) {:display "none"})
               :on-load #(rf/dispatch [:db [state-key [id size]] :status/loaded])
               :on-error #(rf/dispatch [:db [state-key [id size]] :status/error])
               :src (get-url id size)}]]])))
