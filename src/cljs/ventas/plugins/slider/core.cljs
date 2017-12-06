(ns ventas.plugins.slider.core
  (:require
   [ventas.plugins.slider.api :as slider.backend]
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.events :as events]
   [ventas.events.backend :as backend]
   [ventas.components.slider :as components.slider]))

(rf/reg-event-fx
 ::sliders.get
 (fn [cofx [_ kw]]
   {:dispatch [::slider.backend/sliders.get
               {:params {:keyword kw}
                :success #(rf/dispatch [::events/db [::sliders kw] %])}]}))


(defn slider* [{:keys [keyword auto auto-speed]}]
  (let [state-path [::sliders keyword]]
    (when auto
      (js/setInterval #(rf/dispatch [::components.slider/next state-path]) auto-speed))
    (fn [{:keys [slides]}]
      [:div.slider
       [:div.slider__slides {:style {:left @(rf/subscribe [::components.slider/offset state-path])}}
        (for [{:keys [file id]} slides]
          ^{:key id} [:div.slider__slide {:style {:background-image (str "url(" (:url file) ")")}}])]
       [:button.slider__left {:on-click #(rf/dispatch [::components.slider/previous state-path])}
        [base/icon {:name "chevron left"}]]
       [:button.slider__right {:on-click #(rf/dispatch [::components.slider/next state-path])}
        [base/icon {:name "chevron right"}]]])))

(defn slider [kw]
  (rf/dispatch [::sliders.get kw])
  (fn [kw]
    (let [slider-data @(rf/subscribe [::events/db [::sliders kw]])]
      [slider* slider-data])))
