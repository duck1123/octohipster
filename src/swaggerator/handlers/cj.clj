(ns swaggerator.handlers.cj
  (:use [swaggerator.handlers util]
        [swaggerator.link util]
        [swaggerator json util]))

(defn- add-self-link [ctx dk x]
  (assoc x :href (self-link ctx dk x)))

(defn- transform-map [[k v]]
  {:name k, :value (if (map? v) (mapv transform-map v) v)})

(defn- cj-wrap [ctx dk m]
  {:href (un-dotdot (str (or (-> ctx :request :context) "") (self-link ctx dk m)))
   :data (mapv transform-map m)})

(defn wrap-handler-collection-json
  "Wraps handler with a Collection+JSON handler. Note: consumes links;
  requires wrapping the Ring handler with swaggerator.handlers/wrap-collection-json."
  [handler]
  (swap! *handled-content-types* conj "application/vnd.collection+json")
  (fn [ctx]
    (case (-> ctx :representation :media-type)
      "application/vnd.collection+json"
        (let [rsp (handler ctx)
              links (response-links-and-templates rsp)
              dk (:data-key rsp)
              result (dk rsp)
              items (if (map? result)
                      [(-> (cj-wrap ctx dk result)
                           (assoc :links (-> links
                                             links-as-map
                                             (dissoc "self")
                                             (dissoc "listing")
                                             links-as-seq))
                           (assoc :href (:href (get links "self"))))]
                      (map (partial cj-wrap ctx dk) result))
              coll {:version "1.0"
                    :href (if-let [up (get links "listing")]
                            (:href up)
                            (-> ctx :request :uri))
                    :links (if (map? result) [] links)
                    :items items}]
          {:body (jsonify {:collection coll})})
      (handler ctx))))
