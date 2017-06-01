(ns ventas.component
  (:require [fqcss.core :refer [wrap-reagent]]))

(defmacro load-scss []
  (let [ns-name (str (:name (:ns &env)))
        path (clojure.string/replace ns-name "." "-")]
    `(do
       (js/console.log "Loading scss for ns: " ~path)
       (js/console.log "Loading scss for nss: " ~path)
       (require '[ventas.css-loader])
       (ventas.css-loader/update-stylesheet!
         {:full-name ~ns-name
          :href (str "/css/" ~path ".css")}))))