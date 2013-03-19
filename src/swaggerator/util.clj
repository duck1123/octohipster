(ns swaggerator.util
  (:require [clojure.string :as string])
  (:import [com.damnhandy.uri.template UriTemplate]
           [java.net URLEncoder]))

(defn concatv [& xs] (into [] (apply concat xs)))

(defmacro map-to-querystring
  "Turns a map into a query sting, eg. {:abc 123 :def ' '} -> ?abc=123&def=+."
  [m]
  `(if (empty? ~m) ""
     (->> ~m
          (map #(str (-> % key name (URLEncoder/encode "UTF-8"))
                     "="
                     (-> % val str  (URLEncoder/encode "UTF-8"))))
          (interpose "&")
          (apply str "?"))))

(defn alter-query-params [req x]
  (map-to-querystring (merge (:query-params req) x)))

(defn uri-alter-query-params [req x]
  (str (or (:path-info req) (:uri req)) (alter-query-params req x)))

(defn uri-template-for-rel [ctx rel]
  (-> (filter #(= (:rel %) rel) (or ((-> ctx :resource :link-templates)) []))
      first
      :href))

(defn clout->uri-template
  "Turns a Clout route into an RFC 6570 URI Template, eg. /things/:name -> /things/{name}"
  [x] (string/replace x #":([^/]+)" "{$1}"))

(defn expand-uri-template
  "Expands an RFC 6570 URI Template with a map of arguments."
  [tpl x]
  (let [tpl ^UriTemplate (UriTemplate/fromTemplate ^String tpl)]
    (doseq [[k v] x]
      (.set tpl (name k) v))
    (.expand tpl)))

(defn context-relative-uri
  "Returns the full context-relative URI of a Ring request (ie. includes the query string)."
  [req]
  (str (or (:path-info req) (:uri req))
       (if-let [qs (:query-string req)]
         (str "?" qs)
         "")))

(defn full-uri
  "Returns the full context-relative URI of a Ring request (ie. includes the query string)."
  [req]
  (str (:uri req)
       (if-let [qs (:query-string req)]
         (str "?" qs)
         "")))

(defn params-rel
  "Returns a function that expands a URI Template for a specified rel with request params,
  suitable for use as the :see-other parameter in a resource."
  [rel]
  (fn [ctx]
    (let [tpl (uri-template-for-rel ctx rel)]
      (expand-uri-template tpl (-> ctx :request :params)))))

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
