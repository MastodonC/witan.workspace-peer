(ns witan.workspace-peer.tasks.test-tasks
  (:require  [clojure.test :as t]))

(defn append
  [event]
  (str event "-X"))
