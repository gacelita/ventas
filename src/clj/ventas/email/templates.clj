(ns ventas.email.templates)

(defmulti template-body (fn [template _] template))
