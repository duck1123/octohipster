(ns octohipster.mixins
  (:require [clojure.tools.logging :as log]
            [liberator.core :as lib]
            [octohipster.handlers.core :as handler])
  (:use [octohipster pagination problems validator util]
        [octohipster.handlers json edn yaml hal cj util]
        [octohipster.link util]))

(defn validated-resource [r]
  (update-in r [:middleware] conj #(-> %
                                       (wrap-json-schema-validator (:schema r))
                                       (wrap-expand-problems (:handlers r)))))

(defn ok-handler
  [r handler]
  (let [{:keys [presenter data-key handlers]} r]
    (-> (handler presenter data-key)
        (unwrap handlers)
        (wrap-handler-add-clinks)
        handler/wrap-default-handler
        ((fn [handler] (fn [request]
                        (log/debug "Running ok handler")
                        (handler request)) ))
        )))

(defn handled-resource
  "Mixin to add datatype handling"
  ([r]
   (handled-resource r handler/item-handler))
  ([r handler]
   (let [r (merge {:handlers [wrap-handler-json wrap-handler-edn wrap-handler-yaml
                              wrap-handler-hal-json wrap-handler-collection-json]
                   :data-key :data
                   :presenter identity} r)]
     (-> r
         (assoc :handle-ok (ok-handler r handler))
         (assoc :available-media-types (mapcat (comp :ctypes meta) (:handlers r)))))))

(defn item-resource
  "Mixin that includes all boilerplate for working with single items:
  - validation (using JSON schema in :schema for PUT requests)
  - format handling
  - linking to the item's collection"
  [r]
  (log/debug "mixing in item resource")
  (let [r (merge {:method-allowed? (lib/request-method-in :get :put :delete)
                  :collection-key :collection
                  :respond-with-entity? true
                  :new? false
                  :can-put-to-missing? false}
                 r)]
    (-> r
        validated-resource
        (handled-resource handler/item-handler))))

(defn collection-resource
  "Mixin that includes all boilerplate for working with collections of items:
   - validation (using JSON schema in :schema for POST requests)
   - format handling
   - linking to the individual items
   - pagination"
  [r]
  (log/debug "Mixing in collection resource")
  (let [r (merge {:method-allowed? (lib/request-method-in :get :post)
                  :data-key :data
                  :item-key (constantly :item)
                  :post-redirect? true
                  :is-multiple? true
                  :default-per-page 25}
                 r)
        {:keys [item-key count default-per-page]} r]
    (-> r
        (assoc :see-other (params-rel (item-key)))
        (assoc :location (params-rel (item-key)))
        (update-in [:middleware] conj
                   #(wrap-pagination % {:counter count
                                        :default-per-page default-per-page}))
        validated-resource
        (handled-resource handler/collection-handler))))

(defn spy-context
  [r]
  (log/spy :info r))
