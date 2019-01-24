(ns ventas.components.zoomable-image
  (:require
   [js-image-zoom :as zoom]
   [re-frame.core :as rf]
   [reagent.core :as reagent]))

(def state-key ::state)

(rf/reg-event-db
 ::set-loaded
 (fn [db [_ id]]
   (assoc-in db [state-key id :loaded?] true)))

(rf/reg-sub
 ::loaded?
 (fn [db [_ id]]
   (get-in db [state-key id :loaded?])))

(defn- zoom-component [_ _ config]
  (let [image-zoom (atom nil)]
    (reagent/create-class
     {:component-will-unmount #(.kill @image-zoom)
      :display-name "zoom-component"
      :component-did-mount
      (fn [this]
        (reset! image-zoom (zoom. (reagent/dom-node this)
                                  (clj->js config))))
      :reagent-render (fn [id src _]
                        [:div
                         [:img {:src src
                                :onLoad #(rf/dispatch [::set-loaded id])}]])})))

(defn main-view [id size-kw zoomed-size-kw]
  {:pre [(keyword? size-kw)]}
  (let [size @(rf/subscribe [:db [:image-sizes size-kw]])
        loaded? @(rf/subscribe [::loaded? id])]
    (when size
      [:div.zoomable-image (when-not loaded? {:style {:position "absolute"
                                                      :top -9999}})
       ^{:key (hash [loaded? id])}
       [zoom-component id
        (str "images/" id "/resize/" (name zoomed-size-kw))
        {:width (dec (:width size))
         :height (:height size)
         :scale 0.7}]])))
