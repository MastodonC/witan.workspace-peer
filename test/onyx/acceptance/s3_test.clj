(ns onyx.acceptance.s3-test
  (:require [clojure.test :refer :all]
            [schema.test :as st]
            [aero.core :refer [read-config]]
            [clojure.core.async :refer [<!! >!! alts!! timeout chan pipe]]
            [clojure.core.async.lab :refer [spool]]
            [witan.workspace-peer.onyx :as o]
            [io.aviso.exception :as pretty]
            [onyx
             [api]
             [job :refer [add-task]]
             [test-helper :refer [with-test-env load-config]]]
            [onyx.plugin
             [s3 :as s3p]
             [core-async :refer [get-core-async-channels]]]
            [onyx.tasks
             [core-async :as core-async]
             [s3 :as s3]]
            [taoensso.carmine :as car :refer [wcar]])
  (:import [com.amazonaws.services.s3.model S3Object]))

(prefer-method pretty/exception-dispatch clojure.lang.IRecord clojure.lang.IPersistentMap)

(def config (atom {}))

(defn redis-conn []
  {:spec {:uri (get-in @config [:redis-config :redis/uri])}})

(def batch-settings
  {:onyx/batch-size 1
   :onyx/batch-timeout 1000})

(defn replace-config [test-fn]
  (reset! config (merge-with merge (load-config) ; (read-config (clojure.java.io/resource "config.edn") {:profile :test})
                             {:env-config {:onyx/tenancy-id "1"}
                              :peer-config {:onyx/tenancy-id "1"}}
                             {:batch-settings batch-settings}))
  (test-fn))

(use-fixtures :each replace-config)

(defn add-s3-source
  [job]
  (add-task job (s3/reader :in (merge batch-settings
                                      {:s3/bucket "bucket"
                                       :s3/file "file"
                                       :s3/endpoint "eu-west-1"}))))

(defn add-s3-sink
  [job]
  (add-task job (s3/writer :out (merge batch-settings
                                       {:s3/bucket "bucket"
                                        :s3/file "file"
                                        :s3/endpoint "eu-west-1"}))))

(defn run-with-out
  [job]
  (let [job (add-task job (core-async/output :out batch-settings))
        {:keys [env-config
                peer-config]} @config
        {:keys [out in]} (get-core-async-channels job)]
    (with-test-env [test-env [7 env-config peer-config]]
 (prn "Up")
      (onyx.test-helper/validate-enough-peers! test-env job)
      (let [job-id (:job-id (onyx.api/submit-job peer-config job))
_ (prn "submitted")
            result (alts!! [out (timeout 2000)])
_ (prn "DONE")]
        (onyx.api/await-job-completion peer-config job-id)
        (first result)))))

(defn run-with-in
  [job content]
  (let [job (add-task job (core-async/input :in batch-settings))
        {:keys [env-config
                peer-config]} @config
        out (chan)
        {:keys [in]} (get-core-async-channels job)]
    (with-redefs [s3p/put-s3object (fn [_ _ stream] (>!! out (slurp stream)))]
      (with-test-env [test-env [7 env-config peer-config]]
        (pipe (spool [content :done]) in)
        (onyx.test-helper/validate-enough-peers! test-env job)
        (let [job-id (:job-id (onyx.api/submit-job peer-config job))
              result (alts!! [out (timeout 2000)])]
          (onyx.api/await-job-completion peer-config job-id)
          (first result))))))

(defn append
  [event]
  (str event "-X"))

(defn s3-object
  [contents]
  (doto (S3Object.)
    (.setObjectContent (java.io.StringBufferInputStream. contents))))

(deftest mocked-s3-input
  (let [contents "foo"]
    (with-redefs [s3p/get-s3object (constantly (s3-object contents))]
      (is (= (append contents)
             (run-with-out
              (add-s3-source 
               (o/workspace->onyx-job
                {:workflow [[:in :append]
                            [:append :out]]
                 :catalog [{:witan/name :append
                            :witan/fn :onyx.acceptance.s3-test/append}]}
                @config))))))))

(deftest mocked-s3-output
  (let [contents "foo"]
    (is (= (append contents)
           (run-with-in
            (add-s3-sink
             (o/workspace->onyx-job
              {:workflow [[:in :append]
                          [:append :out]]
               :catalog [{:witan/name :append
                          :witan/fn :onyx.acceptance.s3-test/append}]}
              @config))
            contents)))))
