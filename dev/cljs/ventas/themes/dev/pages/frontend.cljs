(ns ventas.themes.dev.pages.frontend
  (:require
   [re-frame.core :as rf]
   [ventas.components.base :as base]
   [ventas.events :as events]
   [ventas.events.backend :as backend]
   [ventas.routes :as routes]))

(def state-key ::state)

(defn page []
  [:div.blank-theme__home
   [base/container
    [:h1 "Test"]]])

(rf/reg-event-fx
 ::init
 (fn [_ _]
   {}))

(routes/define-route!
  :frontend
  {:name "Dev theme - home"
   :url ""
   :component page
   :init-fx [::init]})
