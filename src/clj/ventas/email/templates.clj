(ns ventas.email.templates)

(defmulti template (fn [template _] template))
