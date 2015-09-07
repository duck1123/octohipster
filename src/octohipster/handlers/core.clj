(ns octohipster.handlers.core
  (:use [octohipster.link util]
        [octohipster util])
  (:require [clojure.tools.logging :as log]
            [liberator.representation :refer [ring-response]]))

(defn wrap-ring-response
  [handler]
  (fn [ctx]
    (ring-response (handler ctx))))

(defn wrap-handler-request-links [handler]
  (fn [ctx]
    (-> ctx
        (update-in [:links] concatv (-> ctx :request :links))
        (update-in [:link-templates] concatv (-> ctx :request :link-templates))
        handler)))

(defn wrap-default-handler
  "Wraps a handler with default data transformers"
  [handler]
  (-> handler
      wrap-handler-request-links
      wrap-ring-response))

(defn collection-handler
  "Makes a handler that maps a presenter over data that is retrieved
  from the Liberator context by given data key (by default :data)."
  ([presenter] (collection-handler presenter :data))
  ([presenter k]
   (fn [ctx]
     (-> ctx
         (assoc :data-key k)
         (assoc k (mapv presenter (k ctx)))))))

(defn item-handler
  "Makes a handler that applies a presenter to data that is retrieved
  from the Liberator context by given data key (by default :data)."
  ([presenter] (item-handler presenter :data))
  ([presenter k]
   (fn [ctx]
     (-> ctx
         (assoc :data-key k)
         (assoc k (presenter (k ctx)))))))
