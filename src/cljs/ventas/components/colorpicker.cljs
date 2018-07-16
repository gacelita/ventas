(ns ventas.components.colorpicker
  (:require
   [cljsjs.react-color]
   [re-frame.core :as rf]))

(def chrome-picker (js/React.createFactory js/ReactColor.ChromePicker))

(defn colorpicker [{:keys [on-change]}]
  [:div.colorpicker
   (chrome-picker #js {:disableAlpha false
                       :onChangeComplete (fn [color _]
                                           (rf/dispatch (conj on-change color)))})])