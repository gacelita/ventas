(ns ventas.components.slider
  (:require [fqcss.core :refer [wrap-reagent]]
            [soda-ash.core :as sa]
            [re-frame.core :as rf]))

(rf/reg-sub ::slider
  (fn [db _] (-> db ::slider)))

(rf/reg-event-db ::next
  (fn [db [_]]
    (update-in db [::slider :current-index] #(mod (inc %) 3))))

(rf/reg-event-db ::previous
  (fn [db [_]]
    (update-in db [::slider :current-index] #(mod (dec %) 3))))

(defn slider [{:keys [slides]}]
  (js/setInterval #(rf/dispatch [::next]) 100000)
  (fn [{:keys [slides]}]
    (wrap-reagent
      [:div {:fqcss [::slider]}
        [:div {:fqcss [::slides] :style {:left (* -1 (-> js/window (.-innerWidth)) (:current-index @(rf/subscribe [::slider])))}}
          (for [slide slides]
            [:div {:fqcss [::slide] :style {:background-image (str "url(" (:image slide) ")")}}
              (:content slide)])]
        [:button {:fqcss [::left] :on-click #(rf/dispatch [::previous])}
          [sa/Icon {:name "chevron left"}]]
        [:button {:fqcss [::right] :on-click #(rf/dispatch [::next])}
          [sa/Icon {:name "chevron right"}]]])))