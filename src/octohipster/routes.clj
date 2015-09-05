(ns octohipster.routes
  (:require [clojure.tools.logging :as log]
            [octohipster.core               :refer [gen-doc-resource gen-groups gen-handler]]
            [octohipster.documenters.schema :refer [schema-doc schema-root-doc]]
            [octohipster.handlers.edn       :refer [wrap-handler-edn]]
            [octohipster.handlers.json      :refer [wrap-handler-json]]
            [octohipster.handlers.util      :refer [wrap-apply-encoder wrap-fallback-negotiation]]
            [octohipster.handlers.yaml      :refer [wrap-handler-yaml]]
            [octohipster.host               :refer [wrap-context-bind wrap-host-bind]]
            [octohipster.link.header        :refer [wrap-link-header]]
            [octohipster.link.middleware    :refer [wrap-add-self-link]]
            [octohipster.params.cj          :refer [collection-json-params]]
            [octohipster.params.core        :refer [wrap-params-formats]]
            [octohipster.params.edn         :refer [edn-params]]
            [octohipster.params.json        :refer [json-params]]
            [octohipster.params.yaml        :refer [yaml-params]]
            [octohipster.problems           :refer [wrap-expand-problem-ctype wrap-expand-problems]]
            [octohipster.util               :refer [wrap-cors]]
            [ring.middleware.jsonp          :refer :all]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.nested-params :refer [wrap-nested-params]]
            [ring.middleware.params :refer [wrap-params]]))

(defn routes
  "Creates a Ring handler that routes requests to provided groups
  and documenters."
  [& body]
  (let [defaults {:params [json-params collection-json-params yaml-params edn-params]
                  :documenters [schema-doc schema-root-doc]
                  :groups []
                  :problems {:resource-not-found {:status 404
                                                  :title "Resource not found"}
                             :invalid-data {:status 422
                                            :title "Invalid data"}}}
        options (merge defaults (apply hash-map body))
        {:keys [documenters groups params]} options
        problems (merge (:problems defaults) (:problems options))
        resources (mapcat :resources (gen-groups groups))
        raw-resources (mapcat :resources groups)
        docgen (partial gen-doc-resource
                        (-> options
                            (dissoc :documenters)
                            (assoc :resources raw-resources)))
        resources (concat resources (map docgen documenters))]
    (-> resources gen-handler
                                        ; Links
        wrap-add-self-link
        wrap-link-header
                                        ; Params
        (wrap-params-formats params)
        wrap-keyword-params
        wrap-nested-params
        wrap-params
                                        ; Response
        (wrap-expand-problems problems)
        (wrap-fallback-negotiation [wrap-handler-json wrap-handler-edn wrap-handler-yaml])
        wrap-apply-encoder
        wrap-expand-problem-ctype
                                        ; Headers, bindings, etc.
        wrap-cors
        wrap-json-with-padding
        wrap-context-bind
        wrap-host-bind)))

(defmacro defroutes
  "Creates a Ring handler (see routes) and defines a var with it."
  [n & body] `(def ~n (routes ~@body)))
