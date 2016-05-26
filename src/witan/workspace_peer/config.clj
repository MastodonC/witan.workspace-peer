(ns witan.workspace-peer.config
  (:require [witan.workspace-peer.utils.workflow :refer :all]
            ;;
            [witan.models.dem.ccm.fert.hist-asfr-age]))

(def workflow-namespaces
  ;; witan.models.demography
  ['witan.models.dem.ccm.fert.hist-asfr-age])

(def workflow-fns
  (memoize 
   (fn []
     (mapcat ns-workflowfns workflow-namespaces))))

(def workflow-models
  (memoize 
   (fn []
     (mapcat ns-workflowmodels workflow-namespaces))))
