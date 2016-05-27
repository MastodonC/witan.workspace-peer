(ns witan.workspace-peer.config
  (:require [witan.workspace-peer.utils.workflow :refer :all]
            ;;
            [witan.models.dem.ccm.fert.hist-asfr-age]))

(def workflow-namespaces
  ;; witan.models.demography
  ['witan.models.dem.ccm.fert.hist-asfr-age])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn workflow-fns-
  ([]
   (workflow-fns- workflow-namespaces))
  ([ns]
   (mapcat ns-workflowfns ns)))

(def workflow-fns
  (memoize
   workflow-fns-))

(defn workflow-models-
  ([]
   (workflow-models- workflow-namespaces))
  ([ns]
    (mapcat ns-workflowmodels ns)))

(def workflow-models
  (memoize workflow-models-))

(defn workflow-predicates-
  ([]
   (workflow-predicates- workflow-namespaces))
  ([ns]
   (mapcat ns-workflowpredicates ns)))

(def workflow-predicates
  (memoize workflow-predicates-))
