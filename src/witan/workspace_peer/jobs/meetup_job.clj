(ns witan.workspace-peer.jobs.meetup-job
  (:require [aero.core :refer [read-config]]
            [witan.workspace-peer.tasks
             [core-async :as core-async-task]
             [file-input :as file-input-task]
             [kafka :as kafka-task]
             [meetup-tasks :as meetup]
             [sql :as sql-task]
             [logging :as logging-behavior]
             [metrics :as metrics-behavior]]
            [witan.workspace-peer.utils.job :refer [add-task add-tasks]]
            [onyx.api]))

 (defn build-base-job
  "Build up the base job that will be shared in every environment configuration.
  Here you will build the workflow. Also build catalog entries, flow conditions,
  and lifecycles that apply to your business logic. The idea is to keep this
  clean of environment specific settings."
  [{:keys [batch-size batch-timeout] :as opts}]
  (-> {:workflow [[:read-lines  :extract-meetup-info]
                  [:extract-meetup-info :prepare-rows]
                  [:prepare-rows :write-lines]]
       :lifecycles []
       :catalog []
       :task-scheduler :onyx.task-scheduler/balanced}
      (add-tasks (meetup/meetup-tasks {:onyx/batch-size batch-size
                                       :onyx/batch-timeout batch-timeout}))))

(defn configure-job
  "Configures the job to use either the :dev or :prod environments.
  This is where you will typically switch out your input/output plugins
  or environment specific settings like pinned tasks."
  [job mode {:keys [batch-size] :as opts}]
  (cond-> job
    (= :dev mode) (add-task (core-async-task/output-task :write-lines {:onyx/batch-size batch-size}))
    (= :dev mode) (add-task (file-input-task/input-task :read-lines {:filename "resources/sample_input.edn"}))
    (= :prod mode) (add-task (kafka-task/input-task :read-lines
                                                    {:onyx/batch-size batch-size
                                                     :onyx/max-peers 1
                                                     :kafka/topic "meetup"
                                                     :kafka/group-id "onyx-consumer"
                                                     :kafka/zookeeper "zk:2181"
                                                     :kafka/deserializer-fn :witan.workspace-peer.tasks.kafka/deserialize-message-json
                                                     :kafka/offset-reset :smallest}))
    (= :prod mode) (add-task (sql-task/insert-output :write-lines
                                                     {:onyx/batch-size batch-size
                                                      :sql/classname "com.mysql.jdbc.Driver"
                                                      :sql/subprotocol "mysql"
                                                      :sql/subname "//db:3306/meetup"
                                                      :sql/user "onyx"
                                                      :sql/password "onyx"
                                                      :sql/table :recentMeetups}))
    true (logging-behavior/add-logging :write-lines)
    (= :prod mode) (metrics-behavior/add-timbre-metrics :read-lines)))

(defn build-job [mode]
  (let [batch-size 10
        batch-timeout 1000]
    (-> (build-base-job {:batch-size batch-size :batch-timeout batch-timeout})
        (configure-job mode {:batch-size batch-size :batch-timeout batch-timeout}))))

(defn -main [& args]
  (let [config (read-config (clojure.java.io/resource "config.edn") {:profile :dev})
        peer-config (get config :peer-config)
        job (build-job :prod)]
    (let [{:keys [job-id]} (onyx.api/submit-job peer-config job)]
      (println "Submitted job: " job-id))))
