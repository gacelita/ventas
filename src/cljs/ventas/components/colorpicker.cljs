(ns ventas.components.colorpicker
  (:require
   [cljsjs.react-color]
   [re-frame.core :as rf]))

(def chrome-picker (js/React.createFactory js/ReactColor.ChromePicker))

(defn colorpicker [{:keys [on-change value]}]
  [:div.colorpicker
   (chrome-picker
    (->> {:disableAlpha false
          :color value
          :onChangeComplete (fn [color _]
                              (rf/dispatch (conj on-change (js->clj color :keywordize-keys true))))}
         (remove (fn [[k v]] (nil? v)))
         (into {})
         clj->js))])