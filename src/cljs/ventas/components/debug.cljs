(ns ventas.components.debug
  (:require [ventas.util :as util]
            [re-frame.core :as rf]
            [reagent.core :as reagent]
            [clojure.string :as s]
            [soda-ash.core :as sa]))

(defn bu-debug []
  "Displays the contents of the app database"
  (reagent/with-let [collapsed (reagent/atom true)]
    [:div {:class (s/join " " ["bu" "debugger" (if @collapsed "closed" "open")])
           :on-click #(reset! collapsed (not @collapsed))}
          [:pre @(rf/subscribe [:all])]]))