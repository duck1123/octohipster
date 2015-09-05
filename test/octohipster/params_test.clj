(ns octohipster.params-test
  (:use [ring.mock request]
        [octohipster.params core json cj yaml edn])
  (:require [midje.sweet :refer :all]))

(defn app [req] (select-keys req [:non-query-params :params]))

(facts "json-params"
  (fact "appends params to :non-query-params and :params"
    ((wrap-params-formats app [json-params])
     (-> (request :post "/")
         (content-type "application/json")
         (body "{\"a\":1}"))) =>
         {:non-query-params {:a 1}
          :params {:a 1}}))

(facts "collection-json-params"
  (fact "appends params to :non-query-params and :params"
    ((wrap-params-formats app [collection-json-params])
     (-> (request :post "/")
         (content-type "application/vnd.collection+json")
         (body "{\"template\":{\"data\":[{\"name\":\"a\",\"value\":1}]}}"))) =>
         {:non-query-params {:a 1}
          :params {:a 1}}))

(facts "yaml-params"
  (fact "appends params to :non-query-params and :params"
    (doseq [ctype ["application/yaml" "application/x-yaml"
                   "text/yaml" "text/x-yaml"]]
      ((wrap-params-formats app [yaml-params])
       (-> (request :post "/")
           (content-type ctype)
           (body "{a: 1}"))) =>
           {:non-query-params {:a 1}
            :params {:a 1}})))

(facts "edn-params"
  (fact "appends params to :non-query-params and :params"
    ((wrap-params-formats app [edn-params])
     (-> (request :post "/")
         (content-type "application/edn")
         (body "{:a 1}"))) =>
         {:non-query-params {:a 1}
          :params {:a 1}})

  (facts "does not evaluate clojure"
    ((wrap-params-formats app [edn-params])
     (-> (request :post "/")
         (content-type "application/edn")
         (body "{:a (+ 1 2)}"))) =>
         {:non-query-params {:a '(+ 1 2)}
          :params {:a '(+ 1 2)}}))
