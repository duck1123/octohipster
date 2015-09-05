(ns octohipster.handlers-test
  (:use [octohipster.handlers core util json edn yaml hal cj middleware])
  (:require [clojure.tools.logging :as log]
            [midje.sweet :refer :all]
            [octohipster.json :refer :all]))

(defn wrap-handler-test [handler]
  (fn [ctx] "hello"))

(facts "wrap-handler-json"
  (fact "outputs json for json requests"
    (let [h (-> identity wrap-handler-json wrap-apply-encoder)
          ctx {:representation {:media-type "application/json"}
               :data-key :things
               :things {:a 1}}]
      (h ctx) => (contains {:body "{\"a\":1}"})))

  (fact "does not touch non-json requests"
    (let [h (-> identity wrap-handler-json wrap-apply-encoder)
          ctx {:representation {:media-type "text/plain"}}]
      (h ctx) => ctx)))

(facts "wrap-handler-edn"
  (fact "outputs edn for edn requests"
    (let [h (-> identity wrap-handler-edn wrap-apply-encoder)
          ctx {:representation {:media-type "application/edn"}
               :data-key :things
               :things {:a 1}}]
      (h ctx) => (contains {:body "{:a 1}"})))

  (fact "does not touch non-edn requests"
    (let [h (-> identity wrap-handler-edn wrap-apply-encoder)
          ctx {:representation {:media-type "text/plain"}}]
      (h ctx) => ctx)))

(facts "wrap-handler-yaml"
  (fact "outputs yaml for yaml requests"
    (let [h (-> identity wrap-handler-yaml wrap-apply-encoder)
          ctx {:representation {:media-type "application/yaml"}
               :data-key :things
               :things {:a 1}}]
      (h ctx) => (contains {:body "{a: 1}\n"})))

  (fact "does not touch non-yaml requests"
    (let [h (-> identity wrap-handler-yaml wrap-apply-encoder)
          ctx {:representation {:media-type "text/plain"}}]
      (h ctx) => ctx)))

(facts "wrap-handler-hal-json"
  (fact "consumes links for hal+json requests"
    (let [h (-> identity wrap-handler-hal-json wrap-apply-encoder)
          ctx {:representation {:media-type "application/hal+json"}
               :data-key :things
               :things {:a 1}}]
      (unjsonify (:body (h ctx))) => {:_links {} :a 1}))

  (fact "creates an _embedded wrapper for non-map content and adds templated self links"
    (let [h (-> identity wrap-handler-hal-json wrap-handler-add-clinks wrap-apply-encoder)
          ctx {:representation {:media-type "application/hal+json"}
                                        ; liberator does this constantly thing
               :resource {:clinks (constantly {:entry "/things/{a}"})
                          :item-key (constantly :entry)}
               :data-key :things
               :things [{:a 1}]}]
      (unjsonify (:body (h ctx))) =>
      {:_links {:entry {:templated true
                        :href "/things/{a}"}}
       :_embedded {:things [{:a 1
                             :_links {:self {:href "/things/1"}}}]}}))

  (fact "creates an _embedded wrapper for embed-mapping"
    (let [h (-> identity wrap-handler-hal-json wrap-handler-add-clinks wrap-apply-encoder)
          ctx {:representation {:media-type "application/hal+json"}
               :resource {:embed-mapping (constantly {:things "thing"})
                          :clinks (constantly {:thing "/yo/{a}/things/{b}"})}
               :data-key :yo
               :yo {:a 1 :things [{:b 2}]}}]
      (unjsonify (:body (h ctx))) =>
      {:_links {:thing {:templated true :href "/yo/{a}/things/{b}"}}
       :_embedded {:things [{:b 2
                             :_links {:self {:href "/yo/1/things/2"}}}]}
       :a 1}))

  (fact "does not touch non-hal+json requests"
    (let [h (-> identity wrap-handler-hal-json wrap-apply-encoder)
          ctx {:representation {:media-type "application/json"}}]
      (h ctx) => ctx)))

(facts "wrap-handler-collection-json"
  (fact "consumes links for collection+json requests, to the item if data is a map"
    (let [h (-> identity wrap-handler-collection-json wrap-apply-encoder)
          ctx {:representation {:media-type "application/vnd.collection+json"}
               :links [{:rel "test", :href "/hello"}]
               :data-key :things
               :things {:a 1}}
          ctx2 (assoc ctx :things [{:a 1}])]
      (-> ctx h :body unjsonify :collection :items first :links) =>
      [{:rel "test", :href "/hello"}]
      (-> ctx2 h :body unjsonify :collection :links) =>
      [{:rel "test", :href "/hello"}]))

  (fact "converts nested maps into collection+json format"
    (let [h (-> identity wrap-handler-collection-json wrap-apply-encoder)
          ctx {:representation {:media-type "application/vnd.collection+json"}
               :data-key :things
               :things {:hello {:world 1}}}]
      (-> ctx h :body unjsonify :collection :items first :data) =>
      [{:name "hello"
        :value [{:name "world"
                 :value 1}]}]))

  (fact "does not touch non-collection+json requests"
    (let [h (-> identity wrap-handler-collection-json wrap-apply-encoder)
          ctx {:representation {:media-type "application/json"}}]
      (h ctx) => ctx)))

(facts "item-handler"
  (fact "uses the presenter on the data"
    (let [h (item-handler (partial + 1) :data)]
      (h {:data 1}) => {:data-key :data
                        :data 2})))

(facts "collection-handler"
  (fact "maps the presenter over the data"
    (let [h (collection-handler (partial + 1) :data)]
      (h {:data [1 2]}) => {:data-key :data
                            :data [2 3]})))

(facts "wrap-response-envelope"
  (fact "creates the envelope"
    (let [h (-> identity wrap-handler-json wrap-response-envelope wrap-apply-encoder)]
      (-> {:representation {:media-type "application/json"}
           :data-key :things
           :things [1 2]} h :body unjsonify) =>
           {:things [1 2]}))
  (fact "does not touch non-envelope-able types"
    (let [h (-> identity wrap-handler-hal-json wrap-response-envelope wrap-apply-encoder)]
      (-> {:representation {:media-type "application/hal+json"}
           :data-key :things
           :things {:a 1}} h :body unjsonify) =>
           {:a 1, :_links {}})))
