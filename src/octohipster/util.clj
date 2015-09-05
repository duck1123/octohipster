(ns octohipster.util
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log])
  (:import [com.damnhandy.uri.template UriTemplate]
           [java.net URLEncoder]))

(defn concatv [& xs] (into [] (apply concat xs)))

(defn assoc-map [x k f] (assoc x k (mapv f (k x))))

(defn unwrap [x y] (reduce #(%2 %1) x y))

(defn uri [req] (or (:path-info req) (:uri req)))

(defn uri-template-for-rel
  "Returns the template from the link templates in the context that matches the rel key"
  [ctx rel]
  (-> (filter #(= (:rel %) rel) (or (-> ctx :link-templates) []))
      first
      :href))

(defn uri-template->clout
  "Turns a URI Template into a Clout route, eg. /things/{name} -> /things/:name"
  [x] (string/replace x #"\{([^\}]+)\}" ":$1"))

(defn expand-uri-template
  "Expands an RFC 6570 URI Template with a map of arguments."
  [^String tpl x]
  (let [tpl ^UriTemplate (UriTemplate/fromTemplate tpl)]
    (doseq [[k v] x]
      (.set tpl (name k) (str v)))
    (.expand tpl)))

(defn context-relative-uri
  "Returns the full context-relative URI of a Ring request (ie. includes the query string)."
  [req]
  (str (:context req)
       (uri req)
       (if-let [qs (:query-string req)]
         (str "?" qs)
         "")))

(defn wrap-handle-options-and-head
  "Ring middleware that takes care of OPTIONS and HEAD requests."
  [handler]
  (fn [req]
    (case (:request-method req)
      (:head :options) (-> req
                           (assoc :request-method :get)
                           handler
                           (assoc :body nil))
      (handler req))))

(defn wrap-cors
  "Ring middleware that adds CORS headers."
  [handler]
  (fn [req]
    (let [rsp (handler req)]
      (assoc rsp :headers
             (merge (-> rsp :headers)
                    {"Access-Control-Allow-Origin" "*"
                     "Access-Control-Allow-Headers" "Accept, Authorization, Content-Type"
                     "Access-Control-Allow-Methods" "GET, HEAD, POST, DELETE, PUT"})))))

                                        ; https://groups.google.com/d/msg/clojure-dev/9ctJC-LXNps/JwqpqzkgPyIJ
(defn apply-kw
  "Like apply, but f takes keyword arguments and the last argument is
  not a seq but a map with the arguments for f"
  [f & args]
  {:pre [(map? (last args))]}
  (apply f (apply concat (butlast args) (last args))))
