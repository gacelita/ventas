(ns ventas.utils.logging)

(defn- log [level data]
  {:pre [(#{:info :debug :warn :error} level)]}
  (case level
    :info (apply js/console.info "INFO [] - " data)
    :debug (apply js/console.debug "DEBUG [] - " data)
    :warn (apply js/console.warn "WARN [] - " data)
    :error (apply js/console.error "ERROR [] - " data)
    :trace (apply js/console.trace "TRACE [] - " data)))

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