(ns octohipster.mixins-test
  (:use [octohipster mixins core routes json]
        [ring.mock request])
  (:require [clojure.tools.logging :as log]
            [midje.checking.core :as checking]
            [midje.sweet :refer :all]))

(def post-bin (atom nil))

(def test-schema
  {:id         "Test"
   :properties {:name {:type "string"}}})

(defn post-item
  [ctx]
  (log/info "Posting to test collection")
  (->> ctx :request :non-query-params (reset! post-bin)))

(defn post-exists?
  [ctx]
  (log/info "checking exist")
  {:location "/test/1"
   :status 400
   :things [{:name "a"} {:name "b"}]})

(defresource test-coll
  :mixins [collection-resource]
  :clinks {:item ::test-item}
  :data-key :things
  :exists? post-exists?
  :post! post-item
  :count (constantly 2))

(defresource test-item
  :mixins [item-resource]
  :clinks {:collection ::test-coll}
  :url "/{name}"
  :data-key :thing
  :exists? (fn [ctx] {:thing {:name (-> ctx :request :route-params :name)}}))

(defgroup test-ctrl
  :url "/test"
  :add-to-resources {:schema test-schema}
  :resources [test-coll test-item])

(defroutes test-app
  :groups [test-ctrl])

(facts "collection-resource"
  (fact "outputs data using the presenter and handlers"
    (let [req (-> (request :get "/test")
                  (header "Accept" "application/hal+json"))]

      (let [response (test-app req)]
        response => (contains {:status 200
                               :headers (contains {"Content-Type" "application/hal+json"})})
        (unjsonify (:body response)) =>
        {:_links {:item {:href "/test/{name}" :templated true}
                  :self {:href "/test"}}
         :_embedded {:things [{:_links {:self {:href "/test/a"}} :name "a"}
                              {:_links {:self {:href "/test/b"}} :name "b"}]}})))

  (fact "creates items"
    (let [req (-> (request :post "/test")
                  (header "Accept" "application/json")
                  (content-type "application/json")
                  (body "{\"name\":\"1\"}"))]
      (test-app req) =>
      (contains {:headers (contains {"Location" "/test/1"})
                 :status 200
                 :body (fn [body]
                         (fact
                           body => "null"))})
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
