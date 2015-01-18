(ns octohipster.host)

(def ^:dynamic *host*
  "Current HTTP Host"
  "")

(defn wrap-host-bind
  "Ring middleware that wraps the handler in a binding
  that sets *host* to the HTTP Host header or :server-name
  if there's no Host header."
  [handler]
  (fn [req]
    (let [scheme (or (get-in req [:headers "x-forwarded-proto"])
                     (-> req :scheme name))
          host (or (get-in req [:headers "host"])
                   (-> req :server-name))]
      (binding [*host* (str scheme "://" host)]
        (handler req)))))

(def ^:dynamic *context*
  "Current URL prefix (:context)"
  "")

(defn wrap-context-bind
  "Ring middleware that wraps the handler in a binding
  that sets *context*."
  [handler]
  (fn [req]
    (binding [*context* (:context req)]
      (handler req))))
