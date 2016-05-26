(ns witan.workspace-peer.config
  (:require [witan.workspace-peer.utils.workflow :refer :all]
            ;;
            [witan.models.dem.ccm.fert.hist-asfr-age]))

(def workflow-namespaces
  ;; witan.models.demography
  ['witan.models.dem.ccm.fert.hist-asfr-age])

(defn workflow-fns
  []
  (mapcat ns-workflowfns workflow-namespaces))

(defn workflow-models
  []
  (mapcat ns-workflowmodels workflow-namespaces))
