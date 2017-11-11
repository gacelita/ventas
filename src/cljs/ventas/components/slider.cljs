(ns ventas.components.slider
  (:require
   [re-frame.core :as rf]
   [ventas.components.base :as base]))

(def data-key ::slider)

(rf/reg-event-db
 ::next
 (fn [db [_]]
   (update-in db [data-key :current-index] #(mod (inc %) 3))))

(rf/reg-event-db
 ::previous
 (fn [db [_]]
   (update-in db [data-key :current-index] #(mod (dec %) 3))))

(defn slider [{:keys [slides]}]
  (js/setInterval #(rf/dispatch [::next]) 100000)
  (fn [{:keys [slides]}]
    [:div.slider
     (let [data @(rf/subscribe [:ventas/db [data-key]])]
       [:div.slider__slides {:style {:left (* -1
                                              (-> js/window (.-innerWidth))
                                              (:current-index data))}}
        (for [slide slides]
          [:div.slider__slide {:style {:background-image (str "url(" (:image slide) ")")}}
           (:content slide)])]
       [:button.slider__left {:on-click #(rf/dispatch [::previous])}
        [base/icon {:name "chevron left"}]]
       [:button.slider__right {:on-click #(rf/dispatch [::next])}
        [base/icon {:name "chevron right"}]])]))