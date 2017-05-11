(ns ventas.pages.backend.users.edit
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [re-frame.core :as rf]
            [bidi.bidi :as bidi]
            [re-frame-datatable.core :as dt]
            [soda-ash.core :as sa]
            [taoensso.timbre :as timbre :refer-macros [tracef debugf infof warnf errorf
                                                       trace debug info warn error]]
            [ventas.page :refer [pages]]
            [ventas.pages.backend.users :as users-page]
            [ventas.routes :refer [routes]]
            [ventas.util :refer [go-to dispatch-page-event wrap-sa-with-model]]
            [ventas.components :refer [input-with-model textarea-with-model]]))

(defn images-datatable [action-column sub-key]
  [dt/datatable (keyword (gensym "images")) [sub-key]
    [{::dt/column-key   [:id] ::dt/column-label "#"
      ::dt/sorting      {::dt/enabled? true}}

     {::dt/column-key   [:url] ::dt/column-label "URL"
      ::dt/sorting      {::dt/enabled? true}}

     ; {::dt/column-key   [:tags] ::dt/column-label "Etiquetas"}

     {::dt/column-key   [:actions] ::dt/column-label "Acciones"
      ::dt/render-fn action-column}]

    {::dt/pagination    {::dt/enabled? true
                         ::dt/per-page 5}
    ::dt/table-classes ["ui" "table" "celled"]}])

(defn user-friends []
  (let [action-column
         (fn [_ row]
           [:div
             [sa/Button {:icon true :on-click #(rf/dispatch [:app/entity-remove {:id (:id row)} [:form :friends]])}
               [sa/Icon {:name "remove"}]]])]
      (fn []
        [:div
          [users-page/users-datatable action-column :backend.users.edit/friends]
          [sa/Button "Añadir amigo"]])))

(defn user-images []
  (let [action-column
         (fn [_ row]
           [:div
             [sa/Button {:icon true :on-click #(rf/dispatch [:app/entity-remove {:id (:id row)} [:form :images]])}
               [sa/Icon {:name "remove"}]]])]
      (fn []
        [:div
          [images-datatable action-column :backend.users.edit/images]
          [sa/Button "Añadir imagen"]])))

(defn user-own-images []
  (let [action-column
         (fn [_ row]
           [:div
             [sa/Button {:icon true :on-click #(rf/dispatch [:app/entity-remove {:id (:id row)} [:form :own-images]])}
               [sa/Icon {:name "remove"}]]])]
      (fn []
        [:div
          [images-datatable action-column :backend.users.edit/own-images]
          [sa/Button "Añadir imagen"]])))

(defn user-comments-add [modal-key sub-key key-vec]
    (debug "modal-key" modal-key "key-vec" key-vec)
    (let [atomic-modal (rf/subscribe [sub-key])
          set-open #(rf/dispatch [:backend.users.edit/comments.modal modal-key (assoc @atomic-modal :open %)])
          submit #(do (debug "Sending... (key-vec: " key-vec ")")
                      (-> % .preventDefault)
                        (dispatch-page-event [:comments.modal.submit key-vec (select-keys (:data @atomic-modal) [:id :content])])
                        (set-open false))]
      [sa/Modal {:open (:open @atomic-modal)
                 :close-icon "close" 
                 :on-close #(set-open false)
                 :trigger (reagent/as-element [sa/Button {:on-click #(set-open true)} "Añadir comentario"])}
        [sa/Header {:icon "archive" :content "Editar comentario"}]
        [sa/ModalContent
          [sa/Form
            [sa/TextArea {:on-change #(rf/dispatch [:backend.users.edit/comments.modal modal-key (assoc-in @atomic-modal [:data :content] (-> % .-target .-value))])
                          :placeholder "Contenido del comentario"
                          :default-value (get-in @atomic-modal [:data :content])}]]]
        [sa/ModalActions
          [sa/Button {:color "red" :on-click #(set-open false)}
            [sa/Icon {:name "remove"}]
            "Cancelar"]
          [sa/Button {:type "submit"
                      :color "green"
                      :on-click submit}
            [sa/Icon {:name "checkmark"}]
            "Guardar"]]]))

(defn user-comments []
  (let [action-column
    (fn [_ row]
      [:div
        [sa/Button {:icon true :on-click #(rf/dispatch [:backend.users.edit/comments.edit (:id row) [:form :comments] :comment-modal])}
          [sa/Icon {:name "edit"}]]
        [sa/Button {:icon true :on-click #(rf/dispatch [:app/entity-remove {:id (:id row)} [:form :comments]])}
          [sa/Icon {:name "remove"}]]])]
    (fn []
      [:div
        [dt/datatable
          (keyword (gensym :comments))
          [:backend.users.edit/comments]
          [{::dt/column-key   [:id] ::dt/column-label "#"
            ::dt/sorting      {::dt/enabled? true}}

           {::dt/column-key   [:created-at] ::dt/column-label "Fecha"}

           {::dt/column-key   [:content] ::dt/column-label "Contenido"
            ::dt/sorting      {::dt/enabled? true}}

           {::dt/column-key   [:source] ::dt/column-label "Creador"
            ::dt/render-fn
              (fn [source]
                [:a {:on-click #(go-to routes :backend.users.edit {:id (:id source)})} (:name source)])}

           {::dt/column-key   [:actions] ::dt/column-label "Acciones"
            ::dt/render-fn action-column}]

          {::dt/pagination    {::dt/enabled? true
                               ::dt/per-page 5}
          ::dt/table-classes ["ui" "table" "celled"]}]
        [user-comments-add :comment-modal :backend.users.edit/comment-modal [:form :comments]]
        [:br]
        [:br]])))

(defn user-made-comments []
  (let [action-column
    (fn [_ row]
      [:div
        [sa/Button {:icon true :on-click #(rf/dispatch [:backend.users.edit/comments.edit (:id row) [:form :made-comments] :made-comment-modal])}
          [sa/Icon {:name "edit"}]]
        [sa/Button {:icon true :on-click #(rf/dispatch [:app/entity-remove {:id (:id row)} [:form :made-comments]])}
          [sa/Icon {:name "remove"}]]])]
    (fn []
      [:div
        [dt/datatable
          (keyword (gensym "comments"))
          [:backend.users.edit/made-comments]
          [{::dt/column-key   [:id] ::dt/column-label "#"
            ::dt/sorting      {::dt/enabled? true}}

           {::dt/column-key   [:created-at] ::dt/column-label "Fecha"}

           {::dt/column-key   [:content] ::dt/column-label "Contenido"
            ::dt/sorting      {::dt/enabled? true}}

           {::dt/column-key   [:target] ::dt/column-label "Objetivo"
            ::dt/render-fn
              (fn [target]
                [:a {:on-click #(go-to routes :backend.users.edit {:id (:id target)})} (:name target)])}

           {::dt/column-key   [:actions] ::dt/column-label "Acciones"
            ::dt/render-fn action-column}]

          {::dt/pagination    {::dt/enabled? true
                               ::dt/per-page 5}
          ::dt/table-classes ["ui" "table" "celled"]}]
        [user-comments-add :made-comment-modal :backend.users.edit/made-comment-modal [:form :made-comments]]
        [:br]
        [:br]])))


(defn user-form []
  (let [form @(rf/subscribe [:app/form])
          atomic-form (reagent/atom form)]
      ^{:key form} [sa/Form {:on-submit #(do (-> % .preventDefault) (dispatch-page-event [:submit (do (js/console.log "selecting from" @atomic-form) (select-keys @atomic-form [:name :email :password :description :roles]))]))}
        [sa/FormGroup {:widths "equal"}
          [input-with-model {:label "Nombre" :model atomic-form :name "name"}]
          [input-with-model {:label "Email" :model atomic-form :name "email" :type "email"}]
          [input-with-model {:label "Contraseña" :model atomic-form :name "password" :type "password"}]]
        [sa/FormGroup {:widths "equal"}
          [textarea-with-model {:label "Sobre mí" :model atomic-form :name "description"}]]
        [sa/FormGroup {:widths "equal"}
          [sa/FormField
            [:label "Roles"]
            [sa/Dropdown (wrap-sa-with-model {:model atomic-form :name "roles" :multiple true :fluid true :selection true :options @(rf/subscribe [:app.reference/user.role])})]]]
        [sa/FormButton {:type "submit"} "Enviar"]]))


(defmethod pages :backend.users.edit []
  (fn page-users-edit []
    [:div
      [user-form]
      [sa/Header {:as "h3"} "Comentarios recibidos"]
      [user-comments]
      [sa/Header {:as "h3"} "Comentarios realizados"]
      [user-made-comments]
      [sa/Header {:as "h3"} "Imágenes subidas"]
      [user-own-images]
      [sa/Header {:as "h3"} "Imágenes en las que ha sido etiquetado"]
      [user-images]
      [sa/Header {:as "h3"} "Amigos"]
      [user-friends]
      ]))