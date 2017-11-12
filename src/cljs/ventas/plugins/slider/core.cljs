(ns ventas.plugins.slider.core
  (:require
   [ventas.plugins.slider.api :as api]
   [re-frame.core :as rf]
   [ventas.components.base :as base]))

(rf/reg-event-fx
 ::sliders.get
 (fn [cofx [_ kw]]
   {:dispatch [::api/sliders.get
               {:params {:keyword kw}
                :success-fn #(rf/dispatch [:ventas/db [::sliders kw] %])}]}))

(rf/reg-event-db
 ::next
 (fn [db [_]]
   (update-in db [::state :current-index] #(mod (inc %) 3))))

(rf/reg-event-db
 ::previous
 (fn [db [_]]
   (update-in db [::state :current-index] #(mod (dec %) 3))))

(defn slider* [{:keys [slides]}]
  (js/setInterval #(rf/dispatch [::next]) 100000)
  (fn [{:keys [slides]}]
    [:div.slider
     (let [component-state @(rf/subscribe [:ventas/db [::state]])]
       [:div.slider__slides {:style {:left (* -1
                                              (-> js/window (.-innerWidth))
                                              (:current-index component-state))}}
        (for [{:keys [file]} slides]
          [:div.slider__slide {:style {:background-image (str "url(" (:url file) ")")}}])])
     [:button.slider__left {:on-click #(rf/dispatch [::previous])}
      [base/icon {:name "chevron left"}]]
     [:button.slider__right {:on-click #(rf/dispatch [::next])}
      [base/icon {:name "chevron right"}]]]))

(defn slider [kw]
  (rf/dispatch [::sliders.get kw])
  (fn [kw]
    (let [slider-data @(rf/subscribe [:ventas/db [::sliders kw]])]
      (js/console.log slider-data)
      [slider* slider-data])))
