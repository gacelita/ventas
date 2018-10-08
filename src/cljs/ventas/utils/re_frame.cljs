(ns ventas.utils.re-frame)

(defn pure-subscribe
  "Use subscriptions inside event handlers in a pure way."
  [db sub-and-params]
  @((re-frame.registrar/get-handler :sub (first sub-and-params))
    db
    sub-and-params))