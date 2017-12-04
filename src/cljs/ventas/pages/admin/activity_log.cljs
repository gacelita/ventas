(ns ventas.pages.admin.activity-log
  (:require
   [ventas.routes :as routes]
   [ventas.i18n :refer [i18n]]
   [ventas.pages.admin.skeleton :as admin.skeleton]
   [re-frame.core :as rf]
   [re-frame-datatable.core :as datatable]
   [ventas.components.base :as base]
   [ventas.components.datatable :as components.datatable]
   [ventas.events.backend :as backend]
   [ventas.events :as events]))

(defn- activity-log []
  [:div
   [:p.admin__default-content
    (i18n ::whats-the-activity-log)]
   [:div.activity-log
    ]])

;; Categoría de evento
;; Resumen
;; Fecha
;; Acciones

(def events-key ::events)

(defn- action-column [_ row]
  [:div
   [base/button {:icon true}
    [base/icon {:name "edit"}]]])

(defn table []
  (rf/dispatch [::backend/events.list {:success #(rf/dispatch [::events/db [events-key] %])}])
  (fn []
    (let [id (keyword (gensym))
          subscription [::events/db [events-key]]]
      [:div
       [datatable/datatable id subscription
        [{::datatable/column-key [:id]
          ::datatable/column-label "#"
          ::datatable/sorting {::datatable/enabled? true}}

         {::datatable/column-key [:category]
          ::datatable/column-label (i18n ::category)
          ::datatable/sorting {::datatable/enabled? true}}

         {::datatable/column-key [:abstract]
          ::datatable/column-label (i18n ::abstract)
          ::datatable/sorting {::datatable/enabled? true}}

         {::datatable/column-key [:date]
          ::datatable/column-label (i18n ::date)
          ::datatable/sorting {::datatable/enabled? true}}

         {::datatable/column-key [:actions]
          ::datatable/column-label (i18n ::actions)
          ::datatable/render-fn action-column}]

        {::datatable/pagination {::datatable/enabled? true
                                 ::datatable/per-page 3}
         ::datatable/table-classes ["ui" "table" "celled"]
         ::datatable/empty-tbody-component (fn [] [:p (i18n ::no-products)])}]
       [:div.admin-products__pagination
        [components.datatable/pagination id subscription]]])))

(defn- page []
  [admin.skeleton/skeleton
   [table]])

(routes/define-route!
 :admin.activity-log
 {:name ::page
  :url "activity-log"
  :component page})