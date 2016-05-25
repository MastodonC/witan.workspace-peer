(ns witan.workspace-peer.jobs.meetup-job-test
  (:require [clojure.test :refer [deftest is testing]]
            [schema.core :as s]
            [witan.workspace-peer.launcher.launch-prod-peers :refer [ns-workflowfns]]
            [witan.workspace-api :refer [defworkflowfn]]
            ;;
            [witan.models.dem.ccm.fert.hist-asfr-age]))

(defworkflowfn test-fn
  "This is a test"
  {:witan/name :test
   :witan/version "1.0"
   :witan/exported? true
   :witan/input-schema {:* s/Any}
   :witan/output-schema {:* s/Any}}
  [input params]
  {:foo "bar"})

(deftest ns-resolve-finds-workflow-fns
  (testing "ns-workflowns function"
    (let [[fn metadata] (first (mapcat ns-workflowfns ['witan.workspace-peer.jobs.meetup-job-test]))]
      (is (= fn #'witan.workspace-peer.jobs.meetup-job-test/test-fn))
      (is (= metadata (-> #'test-fn meta :witan/workflowfn)))))
  (testing "an external model"
    (let [result (mapcat ns-workflowfns ['witan.models.dem.ccm.fert.hist-asfr-age])]
      (is result))))
