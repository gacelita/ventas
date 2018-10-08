(ns ventas.components.crud-form
  (:require
   [slingshot.slingshot :refer [throw+]]))

(defmacro field
  "Takes a string or a keyword. Returns a keyword where the ns is the caller ns
   and the name is the given string, or the name of the given keyword.
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