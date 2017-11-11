(ns ventas.pages.admin.configuration.image-sizes
  (:require
   [reagent.core :as reagent :refer [atom]]
   [re-frame.core :as rf]
   [re-frame-datatable.core :as dt]
   [ventas.page :refer [pages]]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [ventas.routes :as routes]
   [ventas.components.base :as base]
   [ventas.components.datatable :as datatable]
   [ventas.i18n :refer [i18n]]))

(def image-sizes-key ::image-sizes)

(defn image-sizes-datatable [action-column]
  (rf/dispatch [:ventas/image-sizes.list [image-sizes-key]])
  (fn [action-column]
    (let [id (keyword (gensym))]
      [:div
       [dt/datatable id [:ventas/db [image-sizes-key]]
        [{::dt/column-key [:id]
          ::dt/column-label "#"
          ::dt/sorting {::dt/enabled? true}}

         {::dt/column-key [:width]
          ::dt/column-label (i18n ::width)}

         {::dt/column-key [:height]
          ::dt/column-label (i18n ::height)
          ::dt/sorting {::dt/enabled? true}}

         {::dt/column-key [:algorithm]
          ::dt/column-label (i18n ::algorithm)
          ::dt/sorting {::dt/enabled? true}}

         {::dt/column-key [:actions]
          ::dt/column-label (i18n ::actions)
          ::dt/render-fn action-column}]

        {::dt/pagination {::dt/enabled? true
                          ::dt/per-page 3}
         ::dt/table-classes ["ui" "table" "celled"]
         ::dt/empty-tbody-component (fn [] [:p (i18n ::no-image-sizes)])}]
       [:div.admin-image-sizes__pagination
        [datatable/pagination id [:ventas/db [image-sizes-key]]]]])))

(defn page []
  [admin.skeleton/skeleton
   (let [action-column
         (fn [_ row]
           [:div
            [base/button {:icon true :on-click #(rf/dispatch [:ventas/entities.remove (:id row)])}
             [base/icon {:name "remove"}]]])]
     [:div.admin__default-content.admin-image-sizes__page
      [image-sizes-datatable action-column]
      [base/button (i18n ::create-image-size)]])])

(routes/define-route!
 :admin.configuration.image-sizes
 {:name (i18n ::page)
  :url "image-sizes"
  :component page})