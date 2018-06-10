(ns ventas.kafka.consumer
  (:require
   [kinsky.client :as kinsky]
   [ventas.kafka :as kafka]
   [ventas.utils :as utils])
  (:import
   [org.apache.kafka.clients.consumer ConsumerRecord]
   [org.apache.kafka.common.errors InterruptException]))

(defn record->data
  [^ConsumerRecord cr]
  {:key (.key cr)
   :offset (.offset cr)
   :partition (.partition cr)
   :topic (.topic cr)
   :value (.value cr)
   :timestamp (.timestamp cr)})

(defn poll [consumer timeout]
  (mapv record->data (.poll @consumer timeout)))

(defn consumer [& [{:keys [group]}]]
  (kinsky/consumer {:bootstrap.servers (kafka/url)
                    :group.id (or group (str (gensym "group")))
                    :auto.offset.reset "earliest"}
                   (kinsky/keyword-deserializer)
                   (kinsky/edn-deserializer)))

(defn consume! [{:keys [min-batch-size consumer-group topics timeout]
                 :or {min-batch-size 10
                      timeout 100}} callback]
  {:pre [topics callback consumer-group]}
  (future
   (let [consumer (consumer {:group consumer-group})
         buffer (volatile! [])]
     (kinsky/subscribe! consumer topics)
     (while (not (Thread/interrupted))
       (utils/interruptible-try
        [InterruptException]
        (let [records (poll consumer timeout)]
          (vswap! buffer into records)
          (when (>= (count @buffer) min-batch-size)
            (callback @buffer)
            (.commitSync @consumer)
            (vreset! buffer []))))))))