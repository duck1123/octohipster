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

(facts "#'octohipster.link.util/params-rel"
  (let [links [[:foo "/foo/{name}"] [:bar "/bar/{name}"]]
        ctx {:resource {:clinks (constantly links)
                        :item-key (constantly :item)}
             :item {:name "bob"}}]

    (fact "when the rel matches one link"
      ((params-rel :foo) ctx) => "/foo/bob")

    (fact "when the rel matches another link"
      ((params-rel :bar) ctx) => "/bar/bob")

    (fact "when the rel matches no links"
      ((params-rel :baz) ctx) => (throws IllegalArgumentException))))
