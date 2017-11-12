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
 (fn [db [_ kw]]
   (let [{:keys [slides]} (get-in db [::sliders kw])]
     (update-in db [::state kw :current-index] #(mod (inc %) (count slides))))))

(rf/reg-event-db
 ::previous
 (fn [db [_ kw]]
   (let [{:keys [slides]} (get-in db [::sliders kw])]
     (update-in db [::state kw :current-index] #(mod (dec %) (count slides))))))

(defn slider* [{:keys [slides keyword auto auto-speed]}]
  (when auto
    (js/setInterval #(rf/dispatch [::next keyword]) auto-speed))
  (fn [{:keys [slides keyword]}]
    [:div.slider
     (let [component-state @(rf/subscribe [:ventas/db [::state keyword]])]
       [:div.slider__slides {:style {:left (* -1
                                              (-> js/window (.-innerWidth))
                                              (:current-index component-state))}}
        (for [{:keys [file id]} slides]
          ^{:key id} [:div.slider__slide {:style {:background-image (str "url(" (:url file) ")")}}])])
     [:button.slider__left {:on-click #(rf/dispatch [::previous keyword])}
      [base/icon {:name "chevron left"}]]
     [:button.slider__right {:on-click #(rf/dispatch [::next keyword])}
      [base/icon {:name "chevron right"}]]]))

(defn slider [kw]
  (rf/dispatch [::sliders.get kw])
  (fn [kw]
    (let [slider-data @(rf/subscribe [:ventas/db [::sliders kw]])]
      [slider* slider-data])))
