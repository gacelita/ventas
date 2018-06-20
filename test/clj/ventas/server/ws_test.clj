(ns ventas.server.ws-test
  (:require
   [chord.channels :refer [bidi-ch]]
   [clojure.core.async :as core.async :refer [<! >! go]]
   [clojure.test :refer [deftest is testing use-fixtures]]
   [ventas.server.ws :as sut]
   [ventas.utils :as utils]))

(use-fixtures :once #(do (sut/start!)
                         (%)))

(deftest call-handler-with-user
  (defmethod sut/handle-request :call-handler-with-user-test [request {:keys [session]}]
    (is (= {:name :call-handler-with-user-test
            :throw? true
            :params {:some :data}}
           request))
    (is (= @session {:user 1})))
  (sut/call-handler-with-user :call-handler-with-user-test
                              {:some :data}
                              {:schema/type :schema.type/user
                               :db/id 1}))

(deftest handle-json-messages
  (let [test-message {:name :handle-json-messages-test
                      :id :test-request
                      :params [:test :data]
                      :type :request}]
    (defmethod sut/handle-request :handle-json-messages-test [message {:keys [channel client-id request session]}]
      (is (= test-message message))
      (is (utils/chan? channel))
      (is (uuid? client-id))
      (is (= [:ws-channel] (keys request)))
      (is (= @session {})))

    (let [read-ch (core.async/chan)
          write-ch (core.async/chan)
          ch (go
               (sut/handle-messages :transit-json {} {:ws-channel (bidi-ch read-ch write-ch)})
               (>! read-ch {:message test-message})
               (is (= {:data true
                       :id :test-request
                       :realtime? false
                       :success true
                       :channel-key nil
                       :type :response}
                      (<! write-ch))))]
      (core.async/alts!! [ch (core.async/timeout 1000)]))))
