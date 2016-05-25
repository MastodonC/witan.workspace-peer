(ns witan.workspace-peer.config
  (:require [witan.workspace-peer.utils.workflow :refer :all]
            ;;
            [witan.models.dem.ccm.fert.hist-asfr-age]))

(def workflowfn-namespaces
  ;; witan.models.demography
  ['witan.models.dem.ccm.fert.hist-asfr-age])

(defn get-functions
  []
  (let [result (mapcat ns-workflowfns workflowfn-namespaces)
        result (map (fn [x] (-> x
                                (update-in [1 :witan/input-schema] str)
                                (update-in [1 :witan/output-schema] str)
                                (update-in [1 :witan/param-] str))) result)]
    result))
