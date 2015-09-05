(ns octohipster.validator-test
  (:use [ring.mock request]
        [octohipster.params core json]
        [octohipster.handlers json edn]
        [octohipster.handlers.util :only [wrap-fallback-negotiation wrap-apply-encoder]]
        [octohipster routes json problems validator])
  (:require [midje.sweet :refer :all]))

(defn handler [req] {:status 200})

(def schema
  {:id "Contact"
   :type "object"
   :properties {:name {:type "string"}}
   :required [:name]})

(def app
  (-> handler
      (wrap-json-schema-validator schema)
      (wrap-params-formats [json-params])
      (wrap-expand-problems {:invalid-data {:status 422
                                            :title "Invalid data"}})
      (wrap-fallback-negotiation [wrap-handler-edn wrap-handler-json])
      wrap-apply-encoder))

(facts "wrap-json-schema-validator"
  (fact "validates POST and PUT requests"
    (-> (request :post "/")
        (content-type "application/json")
        (body (jsonify {:name "aaa"}))
        app :status) => 200
    (-> (request :put "/")
        (content-type "application/json")
        (body (jsonify {:name "aaa"}))
        app :status) => 200
    (-> (request :put "/")
        (content-type "application/json")
        (body (jsonify {:name 1234}))
        app :status) => 422
    (-> (request :post "/")
        (content-type "application/json")
        (body (jsonify {:name 1234}))
        app :status) => 422)
  (fact "uses content negotiation"
    ;; note: not using host binding in test -> no localhost
    (-> (request :post "/")
        (header "Accept" "application/edn")
        (content-type "application/json")
        (body (jsonify {:name 1234}))
        app :body read-string :problemType) => "/problems/invalid-data"))
