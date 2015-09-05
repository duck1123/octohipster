(ns octohipster.link.util
  (:use [octohipster util]
        [org.bovinegenius.exploding-fish :only [normalize-path]])
  (:require [clojure.string :as string]))

(defn prepend-to-href [uri-context l]
  (assoc l :href (normalize-path (str uri-context (:href l)))))

(defn response-links-and-templates [rsp]
  (concatv
   (:links rsp)
   (map #(assoc % :templated true) (:link-templates rsp))))

(defn links-as-map [l]
  (into {}
        (map (fn [x] [(:rel x) (-> x (dissoc :rel))]) l)))

(defn links-as-seq [l]
  (mapv (fn [[k v]] (assoc v :rel k)) l))

(defn clinks-as-map
  "Given a sequence of keyword and template pairs, returns a sequence of keyword and link map pairs"
  [l]
  (->> l
       (apply concat)
       (apply hash-map)
       (map (fn [[k v]] [k {:href v}]))))

(defn params-rel
  "Returns a function that expands a URI Template for a specified rel
  with request params and the item (determined by :item-key) added in post!,
  suitable for use as the :see-other parameter in a resource."
  [rel]
  (fn [ctx]
    (let [tpl (uri-template-for-rel {:link-templates (links-as-seq (clinks-as-map ((:clinks (:resource ctx)))))} rel)
          {:keys [params]} (:request ctx)
          item-key ((-> ctx :resource :item-key))
          vars (merge params (item-key ctx))]
      (expand-uri-template tpl vars))))
