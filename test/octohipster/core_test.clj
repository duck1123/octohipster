(ns octohipster.core-test
  (:require [clojure.tools.logging :as log]
            [clout.core :as clout]
            [ring.mock.request :refer [request]]
            [midje.sweet :refer :all]
            [octohipster.core :refer :all]
            [octohipster.json :refer [jsonify unjsonify]]
            [octohipster.routes :refer [defroutes routes]]))

(facts "defresource"
  (fact "adds the id"
    (defresource aaa :a 1)

    aaa => {:a 1 :id ::aaa}))

(facts "group"
  (fact "adds stuff to resources"

    (group
     :resources [{:a 1} {:a 2}]
     :add-to-resources {:global 0}) =>
     {:resources [{:a 1, :global 0}
                  {:a 2, :global 0}]})

  (fact "applies mixins to resources"
    (group
     :resources [{:a 1, :mixins [#(assoc % :b (:c %))]}]
     :add-to-resources {:c 2}) =>
     {:resources [{:a 1, :b 2, :c 2}]}))

(facts "routes"
  (fact "assembles the ring handler"
    (let [rsrc {:url "/{name}",
                :handle-ok (fn [ctx]
                             (str "Hello " (->
                                            ctx
                                            :request
                                            :route-params
                                            :name)))}
          cntr {:url "/hello", :resources [rsrc]}
          r (routes :groups [cntr])]
      (r (request :get "/hello/me")) => (contains {:body "Hello me"})))

  (fact "replaces clinks"

    (defresource clwhat
      :url "/what")

    (defresource clhome
      :url "/home"
      :clinks {:wat ::clwhat}
      :handle-ok (fn [ctx]
                   (last
                    (first
                     ((-> ctx :resource :clinks))))))

    (defgroup clone
      :url "/one"
      :resources [clhome])

    (defgroup cltwo
      :url "/two"
      :resources [clwhat])

    (defroutes clsite :groups [clone cltwo])

    (clsite (request :get "/one/home")) => (contains {:body "/two/what"}))

  (fact "wraps with middleware"

    (defresource mwhello
      :url "/what"
      :middleware [(fn [handler]
                     (fn [req]
                       (handler (assoc req :from-middleware "hi"))))]
      :handle-ok (fn [ctx] (-> ctx :request :from-middleware)))

    (defroutes mwsite
      :groups [{:url "", :resources [mwhello]}])

    (mwsite (request :get "/what")) => (contains {:body "hi"}))

  (fact "calls documenters"
    (defn dcdocumenter [options]
      (resource
       :url "/test-doc"
       :handle-ok (fn [ctx]
                    (jsonify
                     {:things
                      (map (fn [r]
                             {:url (:url r)})
                           (:resources options))}))))

    (defresource dchello
      :url "/what")

    (defroutes dcsite
      :groups [{:url "", :resources [dchello]}]
      :documenters [dcdocumenter])

    (-> (request :get "/test-doc") dcsite :body unjsonify) =>
    {:things [{:url "/what"}]})

  (fact "returns 404 as a problem"
    (dcsite (request :get "/whatever123"))
    => (contains
        {:status 404
         :body #(= (unjsonify %)
                   {:problemType "http://localhost/problems/resource-not-found"
                    :title "Resource not found"})
         :headers (contains {"content-type" "application/api-problem+json"})})))

(facts "#'octohipster.core/gen-handler"
  (let [resources [{:route (clout/route-compile "/")
                    :handler (fn [request] ..response..)}]
        req {:uri "/"}]
    ((gen-handler resources) req) => ..response..))
