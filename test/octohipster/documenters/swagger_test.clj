(ns octohipster.documenters.swagger-test
  (:use [ring.mock request]
        [octohipster core mixins routes json]
        [octohipster.documenters swagger])
  (:require [midje.sweet :refer :all]))

(def contact-schema
  {:id "Contact"
   :type "object"
   :properties {:guid {:type "string"}}})

(def name-param
  {:name "name"
   :dataType "string"
   :paramType "path"
   :required "true"
   :description "The name of the contact"
   :allowMultiple false})

(def body-param
  {:dataType "Contact"
   :paramType "body"
   :required true
   :allowMultiple false})

(defresource contact-collection
  :mixins [collection-resource]
  :desc "Operations with multiple contacts"
  :methods {:get {:description "Contacts"}}
  :doc {:get {:nickname "getContacts"
              :summary "Get all contacts"}
        :post {:nickname "createContact"
               :summary "Create a contact"
               :parameters [body-param]}})

(defresource contact-item
  :mixins [item-resource]
  :url "/{id}"
  :desc "Operations with individual contacts"
  :doc {:get {:nickname "getContact"
              :summary "Get a contact"
              :parameters [name-param]}
        :put {:nickname "updateContact"
              :summary "Update a contact"
              :parameters [name-param body-param]}
        :delete {:nickname "deleteContact"
                 :summary "Delete a contact"
                 :parameters [name-param]}})

(defgroup contact-group
  :url "/contacts"
  :add-to-resources {:schema contact-schema}
  :resources [contact-collection contact-item])

(defroutes site
  :schemes ["http"]
  :groups [contact-group]
  :documenters [swagger-doc])

(defroutes secure-site
  :schemes ["https"]
  :groups [contact-group]
  :documenters [swagger-doc])

(defn nested-site [req] (site (assoc req :context "/api")))

(facts "swagger-doc"
  (let [request (-> (request :get "/api-docs.json")
                    (header "Accept" "application/json"))]
    (fact "exposes swagger resource listing at /api-docs.json"
      (-> (site request)
          :body unjsonify) =>
          (contains {:swagger "2.0"
                     :schemes ["http"]
                     :info (contains {:version "1.0"})
                     :basePath "http://localhost"
                     :paths
                     (contains
                      {(keyword "/contacts")
                       (contains {:get
                                  (contains {:description "Contacts"})})})}))
    (fact "supports context nesting"
      (-> (nested-site request)
          :body unjsonify) =>
          (contains {:basePath "http://localhost/api"}))))
