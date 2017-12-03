(ns ventas.components.datatable
  (:require
   [re-frame.core :as rf]
   [re-frame-datatable.core :as re-frame-datatable]))

(defn pagination [db-id data-sub]
  (let [pagination-state (rf/subscribe [::re-frame-datatable/pagination-state db-id data-sub])]
    (fn []
      (let [{:keys [::re-frame-datatable/cur-page ::re-frame-datatable/pages]} @pagination-state
            total-pages (count pages)
            next-enabled? (< cur-page (dec total-pages))
            prev-enabled? (pos? cur-page)]

        [:div.ui.pagination.menu
         [:a.item
          {:on-click #(when prev-enabled?
                        (rf/dispatch [::re-frame-datatable/select-prev-page db-id @pagination-state]))
           :class    (when-not prev-enabled? "disabled")}
          [:i.left.chevron.icon]]

         (for [i (range total-pages)]
           ^{:key i}
           [:a.item
            {:class    (when (= i cur-page) "active")
             :on-click #(rf/dispatch [::re-frame-datatable/select-page db-id @pagination-state i])}
            (inc i)])

         [:a.item
          {:on-click #(when next-enabled?
                        (rf/dispatch [::re-frame-datatable/select-next-page db-id @pagination-state]))
           :class    (when-not next-enabled? "disabled")}
          [:i.right.chevron.icon]]]))))