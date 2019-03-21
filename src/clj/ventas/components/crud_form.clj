(ns ventas.components.crud-form
  (:require
   [slingshot.slingshot :refer [throw+]]))

(defmacro field
  "Automates form's `label`
   **ClojureScript only**"
  [state-path {:keys [key] :as args}]
  (if-not (:ns &env)
    `(throw+ {:type ::unsupported-environment
              :message "This macro is cljs-only"})
    (let [caller-ns (str (:name (:ns &env)))
          key (if (sequential? key)
                (first key)
                key)
          kw (keyword caller-ns (name key))]
      `[~'ventas.components.form/field
        (~'merge ~args
         {:db-path ~state-path
          :label (~'ventas.i18n/i18n ~kw)})])))