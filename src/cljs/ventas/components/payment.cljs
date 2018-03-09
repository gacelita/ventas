(ns ventas.components.payment
  "Entry point to payment methods")

(defonce ^:private methods (atom {}))

(defn get-methods []
  @methods)

(defn add-method [kw data]
  (swap! methods assoc kw data))