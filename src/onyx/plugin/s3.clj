(ns onyx.plugin.s3
  (:require [clojure.core.async :as async :refer [timeout]]
            [onyx.peer function
             [pipeline-extensions :as p-ext]]
            [onyx.static
             [default-vals :refer [arg-or-default]]
             [uuid :refer [random-uuid]]]
            [onyx.types :as t]
            [onyx.tasks.redis :as redis]
            [amazonica.core]
            [amazonica.aws.s3 :as s3])
  (:import [java.io ByteArrayInputStream]
           [com.amazonaws.services.s3.model S3Object]))

(def sentinel "done")

;;;;;;;;;;;;;;;;;;;;;
;; Output plugin code

(defn put-s3object
  [creds bucket stream]
  (s3/put-object
   creds
   :bucket-name bucket
   :key "stream"
   :input-stream stream
                                        ; Leaving off length, force the client to buffer and count itself, which is what we would do anyway
                                        ; :metadata {:content-length (count some-bytes)} 
   ))

(defn event->batch
  [event]
  (mapcat :leaves (:tree (:onyx.core/results event))))

(defn event->stream
  [event]
  (when-let [segment (first (event->batch event))]
    (let [^String package (:message segment)]
      (new ByteArrayInputStream (.getBytes package)))))

(defrecord S3Writer [creds bucket file]
  p-ext/Pipeline
  (read-batch [_ event]
    (onyx.peer.function/read-batch event))
  (write-batch [_ event]
    (when-let [stream (event->stream event)]
      (put-s3object creds bucket stream))  
    {})
  (seal-resource [_ _]
    {}))

(defn writer
  [pipeline-data]
  (let [catalog-entry (:onyx.core/task-map pipeline-data)
        {:keys [:s3/access-key
                :s3/secret-key
                :s3/endpoint 
                :s3/bucket 
                :s3/file]} catalog-entry]
    (->S3Writer (cond-> {}
                  access-key (assoc :access-key access-key)
                  secret-key (assoc :secret-key secret-key)
                  endpoint (assoc :endpoint endpoint))
                bucket file)))

;;;;;;;;;;;;;;;;;;;;;
;; Input plugin code

(defn get-s3object
  [creds bucket file]
  (s3/get-object creds bucket file))

(defn get-content
  [^S3Object s3-object]
  (->> s3-object
       .getObjectContent
       (new java.io.InputStreamReader)
       slurp))

(defn onyx-batch
  [content]
  {:onyx.core/batch [(t/input (random-uuid) content)]})

(defrecord S3Reader [creds bucket file state]
  p-ext/Pipeline
  p-ext/PipelineInput
  (write-batch [this event]
    (onyx.peer.function/write-batch event))

  (read-batch [_ _]
    (case @state
      :unsent (let [s3-object (get-s3object creds bucket file)]
                (reset! state :sent)
                (onyx-batch (get-content s3-object)))
      :ackd  (do (reset! state :done)
                 (onyx-batch :done))
      :done nil))

  (ack-segment
      [_ _ _]
      (reset! state :ackd))

  (retry-segment
      [_ _ _]
      (reset! state :unsent))

  (pending?
      [_ _ _]
      (= :sent state))

  (drained?
      [_ _]
      (= :done @state))

  (seal-resource
      [_ _]
      {}))

(defn reader [pipeline-data]
  (let [catalog-entry (:onyx.core/task-map pipeline-data)
        {:keys [:s3/access-key
                :s3/secret-key
                :s3/endpoint 
                :s3/bucket 
                :s3/file]} catalog-entry]
    (->S3Reader (cond-> {}
                  access-key (assoc :access-key access-key)
                  secret-key (assoc :secret-key secret-key)
                  endpoint (assoc :endpoint endpoint))
                bucket 
                file
                (atom :unsent))))
