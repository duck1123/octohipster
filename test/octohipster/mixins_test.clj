(ns octohipster.mixins-test
  (:use [octohipster mixins core routes json]
        [ring.mock request])
  (:require [clojure.tools.logging :as log]
            [midje.checking.core :as checking]
            [midje.sweet :refer :all]))

(def post-bin (atom nil))

(defresource test-coll
  :mixins [collection-resource]
  :clinks {:item ::test-item}
  :data-key :things
  :exists? (fn [ctx] {:things [{:name "a"} {:name "b"}]})
  :post! (fn [ctx] (->> ctx :request :non-query-params (reset! post-bin)))
  :count (constantly 2))

(defresource test-item
  :mixins [item-resource]
  :clinks {:collection ::test-coll}
  :url "/{name}"
  :data-key :thing
  :exists? (fn [ctx] {:thing {:name (-> ctx :request :route-params :name)}}))

(defgroup test-ctrl
  :url "/test"
  :add-to-resources {:schema {:id "Test"
                              :properties {:name {:type "string"}}}}
  :resources [test-coll test-item])

(defroutes test-app
  :groups [test-ctrl])

(facts "collection-resource"
  (fact "outputs data using the presenter and handlers"
    (let [req (-> (request :get "/test")
                  (header "Accept" "application/hal+json"))]
      (test-app req)
      => (every-checker
          (contains {:headers (contains
                               {"Content-Type" "application/hal+json"})})
          (fn [m]
            (checking/extended-=
             (unjsonify (:body m))
             {:_links {:item {:href "/test/{name}" :templated true}
                       :self {:href "/test"}}
              :_embedded {:things [{:_links {:self {:href "/test/a"}} :name "a"}
                                   {:_links {:self {:href "/test/b"}} :name "b"}]}})))))

  (fact "creates items"
    (let [req (-> (request :post "/test")
                  (header "Accept" "application/json")
                  (content-type "application/json")
                  (body "{\"name\":\"1\"}"))]
      (test-app req) => (contains {:headers (contains {"Location" "/test/1"})})
      @post-bin => {:name "1"})))

(facts "item-resource"
  (fact "outputs data using the presenter and handlers"
    (let [req (-> (request :get "/test/hello")
                  (header "Accept" "application/hal+json"))]
      (test-app req)
      => (contains {:body #(= (unjsonify %)
                              {:_links {:collection {:href "/test"}
                                        :self {:href "/test/hello"}}
                               :name "hello"})}))))
