(ns octohipster.util-test
  (:require [midje.sweet :refer :all]
            [octohipster.util :refer :all]))

(facts "uri-template-for-rel"
  (facts "with link templates"
    (let [ctx {:link-templates [{:href "/foo/{name}", :rel :foo}
                                {:href "/bar/{name}", :rel :bar}]}]
      (fact "when matching one"
        (uri-template-for-rel ctx :foo) => "/foo/{name}")
      (fact "when matching another"
        (uri-template-for-rel ctx :bar) => "/bar/{name}")
      (fact "with not matching"
        (uri-template-for-rel ctx :baz) => nil)))
  (fact "without link letmplates"
    (let [ctx {}]
      (uri-template-for-rel ctx :foo) => nil)))

