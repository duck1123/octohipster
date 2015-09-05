(ns octohipster.core
  "Functions and macros for building REST APIs through
  creating resources, groups and routes."
  (:require [liberator.core :as lib]
            [clout.core :as clout]
            [clojure.string :as string]
            [clojure.tools.logging :as log])
  (:use [octohipster util]))

(defn resource
  "Creates a resource. Basically, compiles a map from arguments."
  [& body] (apply hash-map body))

(defmacro defresource
  "Creates a resource and defines a var with it,
  adding the var under :id as a namespace-qualified keyword."
  [n & body] `(def ~n (resource ~@body :id ~(keyword (str *ns* "/" n)))))

(defn group
  "Creates a group, adding everything from :add-to-resources to all
  resources and applying mixins to them."
  [& body]
  (let [c (apply hash-map body)]
    (-> c
        (assoc-map :resources
                   (comp (fn [r] (unwrap (dissoc r :mixins) (:mixins r)))
                         (partial merge (:add-to-resources c))))
        (dissoc :add-to-resources))))

(defmacro defgroup
  "Creates a group and defines a var with it."
  [n & body] `(def ~n (group ~@body)))

(defn gen-resource [r]
  {:url (:url r)
   :handler (unwrap (apply-kw lib/resource r) (:middleware r))})

(defn- make-url-combiner [u]
  (fn [x] (assoc x :url (str u (:url x)))))

(defn all-resources [cs]
  (mapcat #(map (make-url-combiner (:url %)) (:resources %)) cs))

(defn gen-group [resources c]
  (assoc-map c :resources
             (fn [r]
               (-> r
                   (assoc-map :clinks
                              (fn [[k v]]
                                [k (->> resources (filter #(= v (:id %))) first :url)]))
                   gen-resource
                   (assoc :route (-> (str (:url c) (:url r)) uri-template->clout clout/route-compile))
                   (dissoc :url)))))

(defn gen-groups [c]
  (map (partial gen-group (all-resources c)) c))

(defn gen-handler [resources]
  (fn [req]
    (if-let [h (->> resources
                    (map #(assoc % :match (clout/route-matches (:route %) req)))
                    (filter :match)
                    first)]
      (let [{:keys [handler match]} h
            request (assoc req :route-params match)]
        (handler request))
      {:body {}
       :problem :resource-not-found})))

(defn gen-doc-resource [options d]
  (->> (group :url "", :resources [(d options)])
       (gen-group (:groups options))
       :resources first))
