(ns octohipster.core
  "Functions and macros for building REST APIs through
  creating resources, groups and routes."
  (:require [liberator.core :as lib]
            [liberator.representation :as rep]
            [clout.core :as clout]
            [clojure.string :as string]
            [clojure.tools.logging :as log])
  (:use [octohipster util]))

(defmethod rep/render-map-generic "application/hal+json"
  [data context]
  (let [context (assoc-in context [:representation :media-type] "application/json")]
    ;; FIXME: Body should be stripped of elsewhere
    (rep/render-map-generic (:body data) context)))

(defn resource
  "Creates a resource. Basically, compiles a map from arguments."
  [options]
  (identity options))

(defmacro defresource
  "Creates a resource and defines a var with it,
  adding the var under :id as a namespace-qualified keyword."
  [n & {:as options}]
  `(def ~n
     (let [options# (merge ~options {:id ~(keyword (str *ns* "/" n))})]
       (resource options#))))

(defn group
  "Creates a group, adding everything from :add-to-resources to all
  resources and applying mixins to them."
  [options]
  (-> options
      (assoc-map :resources
                 (comp (fn [r] (unwrap (dissoc r :mixins) (:mixins r)))
                       (partial merge (:add-to-resources options))))
      (dissoc :add-to-resources)))

(defmacro defgroup
  "Creates a group and defines a var with it."
  [n & {:as body}]
  (log/debug (str "Creating Group: " n))
  `(def ~n (group ~body)))

(defn handle-resource
  [r ctx]
  (let [middleware (:middleware r)
        r (apply-kw lib/resource r)
        handler (unwrap r middleware)]
    (handler ctx)))

(defn gen-resource [r]
  {:url (:url r)
   :handler (partial handle-resource r)})

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

(defn match-resource
  [req resources]
  (->> resources
       (map #(assoc % :match (clout/route-matches (:route %) req)))
       (filter :match)
       first))

(defn gen-handler [resources]
  (fn [req]
    (if-let [h (match-resource req resources)]
      (do (log/debug (str "Route matched: " (:uri req)))
          (let [{:keys [handler match]} h
             request (assoc req :route-params match)]
         (handler request)))
      {:body {}
       :problem :resource-not-found})))

(defn gen-doc-resource [options d]
  (->> (group {:url "", :resources [(d options)]})
       (gen-group (:groups options))
       :resources first))
