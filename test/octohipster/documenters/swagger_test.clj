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
  :desc "Contacts"
  :add-to-resources {:schema contact-schema}
  :resources [contact-collection contact-item])

(defroutes site
  :groups [contact-group]
  :documenters [swagger-doc swagger-root-doc])

(defn nested-site [req] (site (assoc req :context "/api")))

(facts "swagger-doc"
  (fact "exposes swagger api declarations"
    (let [req (-> (request :get "/api-docs.json/contacts")
                  (header "Accept" "application/json"))]
      (site req) =>
      (contains
       {:body (fn [body]
                (fact
                  (unjsonify body) =>
                  {:apiVersion "1.0"
                   :swaggerVersion "1.1"
                   :basePath "http://localhost"
                   :resourcePath "/contacts"
                   :apis [{:path "/contacts"
                           :description "Operations with multiple contacts"
                           :operations [{:httpMethod "GET"
                                         :nickname "getContacts"
                                         :summary "Get all contacts"
                                         :responseClass "Array[Contact]"}
                                        {:httpMethod "POST"
                                         :nickname "createContact"
                                         :summary "Create a contact"
                                         :responseClass "Contact"
                                         :parameters [body-param]}]}
                          {:path "/contacts/{id}"
                           :description "Operations with individual contacts"
                           :operations [{:httpMethod "GET"
                                         :nickname "getContact"
                                         :summary "Get a contact"
                                         :responseClass "Contact"
                                         :parameters [name-param]}
                                        {:httpMethod "PUT"
                                         :nickname "updateContact"
                                         :summary "Update a contact"
                                         :responseClass "Contact"
                                         :parameters [name-param body-param]}
                                        {:httpMethod "DELETE"
                                         :nickname "deleteContact"
                                         :summary "Delete a contact"
                                         :responseClass "Contact"
                                         :parameters [name-param]}]}]
                   :models {:Contact contact-schema}}))}))))

(facts "swagger-root-doc"
  (fact "exposes swagger resource listing at /api-docs.json"
    (-> (request :get "/api-docs.json")
        (header "Accept" "application/json")
        site :body unjsonify) =>
        {:apiVersion "1.0"
         :swaggerVersion "1.1"
         :basePath "http://localhost"
         :apis [{:path "/contacts"
                 :description "Contacts"}]})
  (fact "supports context nesting"
    (-> (request :get "/api-docs.json")
        (header "Accept" "application/json")
        nested-site :body unjsonify :basePath) => "http://localhost/api"))
