(ns onyx.tasks.redis
  (:require [onyx.schema :as os]
            [schema.core :as s]))

(def UserTaskMapKey
  (os/build-allowed-key-ns :redis))

(s/defn ^:always-validate connected-task
  "Creates a redis connected task, where the first argument
   to the function located at kw-fn is a redis(carmine) connection"
  ([task-name :- s/Keyword opts]
   {:task {:task-map (merge {:onyx/name task-name
                             :onyx/type :function}
                            opts)
           :lifecycles [{:lifecycle/task task-name
                         :lifecycle/calls :onyx.plugin.redis/reader-conn-spec}]}
    :schema {:task-map os/TaskMap
             :lifecycles [os/Lifecycle]}})
  ([task-name :- s/Keyword
    kw-fn :- s/Keyword
    uri :- s/Str
    task-opts :- {s/Any s/Any}]
   (connected-task task-name (merge {:onyx/fn kw-fn
                                     :redis/param? true
                                     :redis/uri uri}
                                    task-opts))))

(def RedisReaderTaskMap
  (s/->Both [os/TaskMap
             {:redis/uri s/Str
              :redis/key (s/either s/Str s/Keyword)
              :redis/cmd s/Keyword
              (s/optional-key :redis/read-timeout-ms) s/Num
              (s/optional-key :redis/length) s/Num
              UserTaskMapKey s/Any}]))

 (comment           :lifecycles [{:lifecycle/task task-name
                                            :lifecycle/calls :onyx.plugin.redis/reader-state-calls}])
(s/defn ^:always-validate reader
  ([task-name :- s/Keyword opts]
   {:task {:task-map (merge {:onyx/name task-name
                             :onyx/plugin :onyx.plugin.redis/reader
                             :onyx/type :input
                             :onyx/medium :redis
                             :onyx/max-peers 1}
                            opts)}
    :schema {:task-map RedisReaderTaskMap
             :lifecycles [os/Lifecycle]}})
  ([task-name :- s/Keyword
    uri :- s/Str
    k :- (s/either s/Str s/Keyword)
    task-opts :- {s/Any s/Any}]
   (reader task-name (merge {:redis/uri uri
                             :redis/key k
                             :redis/cmd :redis/get} task-opts))))

(s/defn ^:always-validate read-list
  ([task-name :- s/Keyword opts]
   {:task {:task-map (merge {:onyx/name task-name
                             :onyx/plugin :onyx.plugin.redis/reader
                             :onyx/type :input
                             :onyx/medium :redis
                             :onyx/max-peers 1}
                            opts)}
    :schema {:task-map RedisReaderTaskMap
             :lifecycles [os/Lifecycle]}})
  ([task-name :- s/Keyword
    uri :- s/Str
    k :- (s/either s/Str s/Keyword)
    c :- s/Num
    task-opts :- {s/Any s/Any}]
   (reader task-name (merge {:redis/uri uri
                             :redis/key k
                             :redis/length c
                             :redis/cmd :redis/lrange}
                            task-opts))))

(def RedisWriterTaskMap
  (s/->Both [os/TaskMap
             {:redis/uri s/Str
              :redis/key (s/either s/Str s/Keyword)
              :redis/cmd s/Keyword
              (s/optional-key :redis/read-timeout-ms) s/Num
              UserTaskMapKey s/Any}]))

(s/defn ^:always-validate writer
  ([task-name :- s/Keyword opts]
   {:task {:task-map (merge {:onyx/name task-name
                             :onyx/plugin :onyx.plugin.redis/writer
                             :onyx/type :output
                             :onyx/medium :redis}
                            opts)}
    :schema {:task-map RedisWriterTaskMap}})
  ([task-name :- s/Keyword
    uri :- s/Str
    cmd :- s/Keyword
    k :- (s/either s/Str s/Keyword)
    task-opts :- {s/Any s/Any}]
   (writer task-name (merge {:redis/cmd cmd
                             :redis/uri uri
                             :redis/key k}
                            task-opts))))
