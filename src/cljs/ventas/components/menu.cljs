(ns ventas.components.menu
  (:require
   [ventas.components.base :as base]
   [cljs.spec.alpha :as spec]
   [ventas.utils :as utils]))

(spec/def ::href string?)
(spec/def ::text string?)
(spec/def ::target string?)
(spec/def ::children
  (spec/coll-of ::item))

(spec/def ::item
  (spec/keys :opt-un [::href
                      ::text
                      ::children
                      ::target]))

(spec/def ::items
  (spec/coll-of ::item))

(defn menu-children [children]
  [:div.menu__children
   (for [{:keys [id text href target] :as child} children]
     ^{:key (or id (hash child))}
     [:a.menu__child {:href href
                      :target target} text])])

(defn menu-item [{:keys [href target text children] :as a} current?]
  [:li.menu__item
   {:class (when current? "menu__item--active")}
   [:a {:href href
        :target target}
    text]
   (when children
     [:div.menu__overlay
      [base/container
       [menu-children children]]])])

(defn menu [{:keys [current-fn items]}]
  {:pre [(utils/check ::items items)]}
  [:div.menu
   [base/container
    [:ul.menu__items
     (doall
      (for [item items]
        ^{:key (or (:id item) (hash item))}
        [menu-item item (current-fn item)]))]]])