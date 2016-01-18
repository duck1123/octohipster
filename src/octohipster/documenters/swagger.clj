(ns octohipster.documenters.swagger
  (:use [octohipster core host mixins])
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]))

(def api-version "1.0")
(def swagger-version "2.0")

(defn parameter-map
  [resource method]
  (if-let [params (concat (get resource :parameters [])
                          (get-in resource [:methods (keyword method) :parameters] []))]
    {:parameters
     (map
      (fn [[n m]]
        (merge m {:name (name n)
                  :required (:required m (= (:in m) "path"))}))
      params)}))

(defn swagger-operation
  [g resource method]
  (merge {:summary (get-in resource [:methods method :summary] "")
          :tags (filter identity [(or (:name g) (:url g))])
          :produces (:available-media-types resource)
          :responses (or (get-in resource [:methods method :responses])
                         {"200" {:description "Default Response"}})}
         (parameter-map resource (name method))
         (if-let [desc (or (get-in resource [:methods method :description])
                           (:description resource))]
           {:description desc})))

(defn swagger-resource
  [g resource]
  (let [url (str (:url g) (:url resource))]
    [[url
      (merge
       (when (:handle-ok resource)
         {"get" (swagger-operation g resource :get)})
       (when (:post! resource)
         {"post" (merge {:summary (get-in resource [:methods :post :summary] "")
                         :tags (filter identity [(or (:name g) (:url g))])
                         :produces (:available-media-types resource)
                         :responses (or (get-in resource [:methods :post :responses])
                                        {"200" {:description "Default Response"}})}
                        (parameter-map resource "post")
                        (if-let [description (get-in resource [:methods :post :description])]
                          {:description description}))})
       (when (:delete! resource)
         {"delete" (merge {:summary (get-in resource [:methods :delete :summary] "")
                           :tags (filter identity [(or (:name g) (:url g))])
                           :produces (:available-media-types resource)
                           :responses {"200" {:description "Response"}}}
                          (parameter-map resource "delete")
                          (if-let [description (get-in resource [:methods :delete :description])]
                            {:description description}))}))]]))

(defn swagger-group
  [g]
  (->> (:resources g)
       (sort-by :name)
       (mapcat (partial swagger-resource g))))

(defn swagger-root
  "Generates a Swagger 2.0 API doc"
  [ctx options]
  {:swagger swagger-version
   :info {:title (:name options)
          :version api-version}
   :schemes (:schemes options [(name (get-in ctx [:request :scheme] :https))])
   :basePath (str (get-in ctx [:request :context]) "/")
   :host (or (get-in ctx [:request :headers "x-forwarded-host"])
             (get-in ctx [:request :headers "host"])
             "localhost")
   :paths (->> (:groups options)
               (sort-by :name)
               (mapcat swagger-group)
               (into (sorted-map)))})

(defn swagger-doc
  "Documenter Middleware"
  [options]
  (resource
   {:url "/api-docs.json"
    :mixins [handled-resource]
    :exists? (fn [ctx] {:data (swagger-root ctx options)})}))
