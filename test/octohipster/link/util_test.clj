(ns octohipster.link.util-test
  (:require [midje.sweet :refer :all]
            [octohipster.link.util :refer :all]))

(facts "clinks-as-map"
  (fact "when given multiple clinks"
    (let [l [[:foo  "/foo/{name}"]
             [:bar "/bar/{name}"]]]
      (clinks-as-map l) =>
      (contains [[:foo  {:href "/foo/{name}"}]
                 [:bar {:href "/bar/{name}"}]]
                :in-any-order))))
