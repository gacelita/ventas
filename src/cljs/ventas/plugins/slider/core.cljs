(ns ventas.plugins.slider.core
  (:require
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.components.slider :as components.slider]
   [ventas.events :as events]
   [ventas.events.backend :as backend]
   [ventas.plugins.slider.api :as slider.backend]))

(def state-key ::state)

(rf/reg-event-fx
 ::sliders.get
 (fn [cofx [_ kw]]
   {:dispatch [::slider.backend/sliders.get
               {:params {:keyword kw}
                :success [::sliders.get.next kw]}]}))

(rf/reg-event-db
 ::sliders.get.next
 (fn [db [_ kw {:keys [slides auto-speed auto keyword]}]]
   (assoc-in db [state-key kw]
             {:slides (mapv (fn [image]
                              (merge image
                                     {:width ::components.slider/viewport
                                      :height 350}))
                            slides)
              :orientation :horizontal
              :render-index (dec (count slides))
              :current-index 1
              :visible-slides 1
              :auto auto
              :auto-speed auto-speed
              :keyword keyword})))

(defn slider* [{:keys [keyword auto auto-speed]}]
  (let [state-path [state-key keyword]]
    (when auto
      (js/setInterval #(rf/dispatch [::components.slider/next state-path]) auto-speed))
    (fn [{:keys [slides]}]
      ^{:key @(rf/subscribe [::events/db (conj state-path :render-index)])}
      [:div.slider
       [:div.slider__slides {:style {:left @(rf/subscribe [::components.slider/offset state-path])}}
        (map-indexed
         (fn [idx {:keys [file id]}]
           ^{:key idx}
           [:div.slider__slide {:style {:background-image (str "url(" (:url file) ")")}}])
         @(rf/subscribe [::components.slider/slides state-path]))]
       (when (pos? (count slides))
         [:div
          [:button.slider__left {:on-click #(rf/dispatch [::components.slider/previous state-path])}
           [base/icon {:name "chevron left"}]]
          [:button.slider__right {:on-click #(rf/dispatch [::components.slider/next state-path])}
           [base/icon {:name "chevron right"}]]])])))

(defn slider [kw]
  (rf/dispatch [::sliders.get kw])
  (fn [kw]
    (let [slider-data @(rf/subscribe [::events/db [state-key kw]])]
      (when (:keyword slider-data)
        [slider* slider-data]))))
