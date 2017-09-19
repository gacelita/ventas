(ns ventas.components.datatable
  (:require
   [re-frame.core :as rf]))

(defn pagination [db-id data-sub]
  (let [pagination-state (rf/subscribe [::re-frame-datatable.core/pagination-state db-id data-sub])]
    (fn []
      (let [{:keys [::re-frame-datatable.core/cur-page ::re-frame-datatable.core/pages]} @pagination-state
            total-pages (count pages)
            next-enabled? (< cur-page (dec total-pages))
            prev-enabled? (pos? cur-page)]

        [:div.ui.pagination.menu
         [:a.item
          {:on-click #(when prev-enabled?
                        (rf/dispatch [::re-frame-datatable.core/select-prev-page db-id @pagination-state]))
           :class    (when-not prev-enabled? "disabled")}
          [:i.left.chevron.icon]]

         (for [i (range total-pages)]
           ^{:key i}
           [:a.item
            {:class    (when (= i cur-page) "active")
             :on-click #(rf/dispatch [::re-frame-datatable.core/select-page db-id @pagination-state i])}
            (inc i)])

         [:a.item
          {:on-click #(when next-enabled?
                        (rf/dispatch [::re-frame-datatable.core/select-next-page db-id @pagination-state]))
           :class    (when-not next-enabled? "disabled")}
          [:i.right.chevron.icon]]]))))