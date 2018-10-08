(ns ventas.components.colorpicker
  (:require
   [react-color]
   [re-frame.core :as rf]))

(def chrome-picker (js/React.createFactory (.-ChromePicker react-color)))

(defn colorpicker [{:keys [on-change value]}]
  [:div.colorpicker
   (chrome-picker
    (->> {:disableAlpha false
          :color value
          :onChangeComplete (fn [color _]
                              (rf/dispatch (conj on-change (:hex (js->clj color :keywordize-keys true)))))}
         (remove (fn [[k v]] (nil? v)))
         (into {})
         clj->js))])

(defn colorpicker-input [{:keys [on-change value]}]
  [:div.colorpicker-input
   [:input {:value (or value "")}]
   [colorpicker {:on-change on-change
                 :value value}]])