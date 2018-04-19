(ns ventas.email.elements
  (:require
   [clojure.java.io :as io]
   [ventas.i18n :refer [i18n]]
   [ventas.entities.configuration :as entities.configuration]
   [ventas.entities.user :as entities.user]
   [ventas.database.entity :as entity]
   [ventas.config :as config]))

(defn get-url [s]
  (let [{:keys [host port]} (config/get :server)]
    (str "http://" host ":" port s)))

(defn logo-html []
  [:img {:width 60
         :src (get-url "/files/logo")}])

(defn table [args & content]
  [:table (merge {:cellSpacing 0
                  :cellPadding 0
                  :border 0
                  :style {:border "0px"}}
                 args)
   [:tbody
    content]])

(defn wrapper [& content]
  [:html {:xmlns "http://www.w3.org/1999/xhtml"}
   [:head
    [:style {:type "text/css"}
     (slurp (io/resource "public/files/css/email.css"))]]
   [:body
    (table
     {:width "100%"
      :height "100%"
      :id "bodyTable"}
     [:tr
      [:td
       (table
        {:width 600
         :cellPadding 20
         :id "emailContainer"}
        [:tr
         [:td {:align "center"
               :style {:text-align "center"}}
          content]])]])]])

(defn header [attrs name]
  [:th.table-header attrs
   name])

(defn get-user-culture [user]
  (:i18n.culture/keyword (entity/find (:user/culture user))))

(defn skeleton [user & content]
  (let [culture-kw (get-user-culture user)]
    [:div
     (wrapper
      (table
       {:width "100%"}
       [:tr
        [:td.logo {:align "center"
                   :style {:text-align "center"}}
         [:img {:src "/files/logo"}]
         [:h2 (entities.configuration/get :site.title)]]]
       [:tr
        [:td {:style {:text-align "left"}
              :align "left"}
         [:h3 (i18n culture-kw ::hello (entities.user/get-name (:db/id user)))]
         content
         [:br]
         [:p (i18n culture-kw ::go-to-orders)
          " "
          [:a {:href (get-url "/profile/orders")}
           (i18n culture-kw ::go-to-orders-link)]]]]))]))