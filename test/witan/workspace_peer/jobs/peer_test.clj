(ns witan.workspace-peer.jobs.peer-test
  (:require [clojure.test :refer [deftest is testing]]
            [schema.core :as s]
            [witan.workspace-peer.config :as c]
            [witan.workspace-api :refer [defworkflowfn defworkflowmodel defworkflowpred]]))

(defworkflowfn test-fn
  "This is a test"
  {:witan/name :test
   :witan/version "1.0"
   :witan/exported? true
   :witan/input-schema {:* s/Any}
   :witan/output-schema {:* s/Any}}
  [input params]
  {:foo "bar"})

(defworkflowmodel test-model
  "doc"
  {:witan/name :default
   :witan/version "1.0"}
  [[:in :out]
   [:shake [:pred :left :right]]
   [:all :about]])

(defworkflowpred test-pred
  "pred doc"
  {:witan/name :pred
   :witan/version "1.0"
   :witan/exported? true
   :witan/input-schema {:number s/Num}
   :witan/param-schema {:value s/Num}}
  [{:keys [number]} {:keys [value]}]
  (< number value))


(deftest ns-resolve-finds-workflow-fns
  (testing "workflow-fns function"
    (let [[fn metadata] (first (c/workflow-fns ['witan.workspace-peer.jobs.peer-test]))]
      (is (= fn "#'witan.workspace-peer.jobs.peer-test/test-fn"))
      (is (= metadata (-> #'test-fn meta :witan/workflowfn)))))
  (testing "workflow-modes function"
    (let [[model metadata] (first (c/workflow-models ['witan.workspace-peer.jobs.peer-test]))]
      (is (= model "#'witan.workspace-peer.jobs.peer-test/test-model"))
      (is (= metadata (-> #'test-model meta :witan/workflowmodel)))))
  (testing "workflow-preds function"
    (let [[model metadata] (first (c/workflow-predicates ['witan.workspace-peer.jobs.peer-test]))]
      (is (= model "#'witan.workspace-peer.jobs.peer-test/test-pred"))
      (is (= metadata (-> #'test-pred meta :witan/workflowpred)))))
  (testing "an external model"
    (let [result (c/workflow-fns)]
      (is result))))
