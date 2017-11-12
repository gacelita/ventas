(ns ventas.plugins.slider.api
  (:require
   [re-frame.core :as rf]))

(rf/reg-event-fx
 ::sliders.get
 (fn [cofx [_ options]]
   {:ws-request (merge {:name :ventas.plugins.slider.core/sliders.get} options)}))
