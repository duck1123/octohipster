(ns octohipster.documenters.schema-spec
  (:use [ring.mock request]
        [octohipster core routes json]
        [octohipster.documenters schema])
  (:require [midje.sweet :refer :all]))

(def contact-schema
  {:id "Contact"
   :type "object"
   :properties {:guid {:type "string"}}})

(defresource contact-collection)
(defresource contact-entry :url "/{id}")
(defgroup contact-group
  :url "/contacts"
  :add-to-resources {:schema contact-schema}
  :resources [contact-collection contact-entry])
(defroutes site
  :groups [contact-group]
  :documenters [schema-doc schema-root-doc])

(facts "schema-doc"
  (fact "exposes schemas at /schema"
    (-> (request :get "/schema")
        (header "Accept" "application/hal+json")
        site :body unjsonify) =>
        {:_links {:self {:href "/schema"}}
         :Contact contact-schema}))

(facts "schema-root-doc"
  (fact "exposes schemas and groups at /"
    (-> (request :get "/")
        (header "Accept" "application/hal+json")
        site :body unjsonify) =>
        {:_links {:self {:href "/"}
                  :contacts {:href "/contacts"}}
         :_embedded {:schema {:_links {:self {:href "/schema"}}
                              :Contact contact-schema}}}))
