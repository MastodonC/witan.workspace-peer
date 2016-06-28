(ns onyx.tasks.s3
  (:require [onyx.schema :as os]
            [schema.core :as s]))

(def UserTaskMapKey
  (os/build-allowed-key-ns :s3))

(def S3TaskMap
  (s/->Both [os/TaskMap
             {(s/optional-key :s3/access-key) s/Str ; These should come from the environment
              (s/optional-key :s3/secret-key) s/Str
              :s3/endpoint s/Str
              :s3/bucket s/Str
              :s3/file s/Str
           ;   (s/optional-key :redis/read-timeout-ms) s/Num ; Add some optional time outs, etc
              UserTaskMapKey s/Any}]))

(s/defn ^:always-validate reader
  ([task-name :- s/Keyword opts]
   {:task {:task-map (merge {:onyx/name task-name
                             :onyx/plugin :onyx.plugin.s3/reader
                             :onyx/type :input
                             :onyx/medium :s3
                             :onyx/max-peers 1}
                            opts)}
    :schema {:task-map S3TaskMap
             :lifecycles [os/Lifecycle]}})
  ([task-name :- s/Keyword
    access-key :- s/Str
    secret-key :- s/Str
    endpoint :- s/Str
    bucket :- s/Str
    file :- s/Str
    task-opts :- {s/Any s/Any}]
   (reader task-name (merge {:s3/access-key access-key
                             :s3/secret-key secret-key
                             :s3/endpoint endpoint
                             :s3/bucket bucket
                             :s3/file file} task-opts))))

(s/defn ^:always-validate writer
  ([task-name :- s/Keyword opts]
   {:task {:task-map (merge {:onyx/name task-name
                             :onyx/plugin :onyx.plugin.s3/writer
                             :onyx/type :output
                             :onyx/medium :s3
                             :onyx/max-peers 1}
                            opts)}
    :schema {:task-map S3TaskMap
             :lifecycles [os/Lifecycle]}})
  ([task-name :- s/Keyword
    access-key :- s/Str
    secret-key :- s/Str
    endpoint :- s/Str
    bucket :- s/Str
    file :- s/Str
    task-opts :- {s/Any s/Any}]
   (reader task-name (merge {:s3/access-key access-key
                             :s3/secret-key secret-key
                             :s3/endpoint endpoint
                             :s3/bucket bucket
                             :s3/file file} task-opts))))
