(ns octohipster.link-test
  (:use ring.mock.request
        octohipster.link.header)
  (:require [midje.sweet :refer :all]))

(facts "make-link-header"
  (fact "makes the link header"
    (make-link-header []) => nil
    (make-link-header [{:href "/hello" :rel "next"}]) => "</hello>; rel=\"next\""
    (make-link-header [{:href "/hello" :rel "next"}
                       {:href "/test" :rel "root" :title "thingy"}]) =>
                       "</hello>; rel=\"next\", </test>; rel=\"root\" title=\"thingy\""))

(facts "wrap-link-header"
  (fact "does not return the header when there are no :links"
    (let [app (wrap-link-header (fn [req] {:status 200 :headers {} :links [] :body ""}))]
      (get (-> (request :get "/") app :headers) "Link") => nil))
  (fact "returns the header when there are :links"
    (let [app (wrap-link-header (fn [req] {:status 200 :headers {} :links [{:rel "a" :href "b"}] :body ""}))]
      (get (-> (request :get "/") app :headers) "Link") => "<b>; rel=\"a\"")))
