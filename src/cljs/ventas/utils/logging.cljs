(ns ventas.utils.logging
  "Simple wrapper for js/console")

(def log-level :debug)

(def ^:private log-levels [:trace :debug :info :warn :error])

(defn- log [level data]
  {:pre [(contains? (set log-levels) level)]}
  (let [index (.indexOf log-levels level)
        min-index (.indexOf log-levels log-level)]
    (when (<= min-index index)
      (case level
        :trace (apply js/console.log "TRACE [] - " data)
        :debug (apply js/console.log "DEBUG [] - " data)
        :info (apply js/console.info "INFO [] - " data)
        :warn (apply js/console.warn "WARN [] - " data)
        :error (apply js/console.error "ERROR [] - " data)))))

(defn trace [& data]
  (log :trace data))

(defn debug [& data]
  (log :debug data))

(defn info [& data]
  (log :info data))

(defn warn [& data]
  (log :warn data))

(defn error [& data]
  (log :error data))

