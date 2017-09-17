(ns ventas.utils.logging)

(defn- log [level data]
  {:pre [(#{:info :debug :warn :error} level)]}
  (case level
    :info (apply js/console.info "INFO [] - " data)
    :debug (apply js/console.debug data)
    :warn (apply js/console.warn data)
    :error (apply js/console.error data)
    :trace (apply js/console.trace data)))

(defn info [& data]
  (log :info data))

(defn debug [& data]
  (log :debug data))

(defn warn [& data]
  (log :warn data))

(defn error [& data]
  (log :error data))

(defn trace [& data]
  (log :trace data))