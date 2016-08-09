(ns onyx.integration.s3-test
  (:require [amazonica.aws.s3 :as s3c]
            [clojure.test :refer :all]
            [schema.test :as st]
            [aero.core :refer [read-config]]
            [clojure.core.async :refer [<!! >!! alts!! timeout chan pipe]]
            [clojure.core.async.lab :refer [spool]]           
            [io.aviso.exception :as pretty]
            [onyx
             [api]
             [job :refer [add-task]]]
            [onyx.plugin
             [s3 :as s3p]
             [core-async :refer [get-core-async-channels]]]
            [onyx.tasks
             [core-async :as core-async]
             [s3 :as s3]])
  (:import [com.amazonaws.services.s3.model S3Object]
           [java.io ByteArrayInputStream]))

(defn uuid
  []
  (str (java.util.UUID/randomUUID)))

(defn unique-s3-file-name
  []
  (str "onyx-s3-plugin/" (uuid)))

(def peer-config
  {:zookeeper/address "localhost:2181"
   :onyx/tenancy-id "1"
   :onyx.peer/job-scheduler :onyx.job-scheduler/balanced
   :onyx.messaging/impl :aeron
   :onyx.messaging/peer-port 40200
   :onyx.messaging/bind-addr "localhost"})

(def creds {;:access-key "ADD_YOUR_OWN" ;ditto for below
            ;:secret-key "ADD_YOUR_OWN"
            :endpoint "eu-central-1"})

(def bucket "mc-integration-testing")

(def test-data "s3-onyx-plugin-test-data")

(def base-job
  {:s3/bucket bucket
   :s3/endpoint "eu-west-1"
   :onyx/batch-size 1
   :onyx/batch-timeout 1000})

(defn create-input-file
  [file]
  (s3c/put-object
   creds
   :bucket-name bucket
   :key file
   :input-stream (new ByteArrayInputStream (.getBytes test-data))))

(defn get-output
  [file]
  (->> (s3c/get-object creds bucket file)
       :object-content
       (new java.io.InputStreamReader)
       slurp))

(defn submit-job
  [in-file out-file]
  (-> {:workflow [[:in :out]]
       :task-scheduler :onyx.task-scheduler/balanced}
      (add-task (s3/reader :in 
                           (merge base-job
                                  {:s3/file in-file
                                   :s3/access-key ""
                                   :s3/secret-key ""
                                   :s3/endpoint "eu-west-1"})))
      (add-task (s3/writer :out
                           (merge base-job
                                  {:s3/file out-file
                                   :s3/access-key ""
                                   :s3/secret-key ""
                                   :s3/endpoint "eu-west-1"})))
      ((partial onyx.api/submit-job peer-config))))

(deftest ^:integration s3-plugin-works
  (let [in-file (unique-s3-file-name)
        _ (create-input-file in-file)
        out-file (unique-s3-file-name)
        job-details (submit-job in-file out-file)
        _ (onyx.api/await-job-completion peer-config (:job-id job-details))
        out-content (get-output out-file)]
    (is
     (= test-data
        out-content))))
