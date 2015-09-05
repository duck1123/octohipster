(ns octohipster.link.header
  (:use [octohipster util]
        [octohipster.link util]))

(defn- make-link-header-field [[k v]]
  (format "%s=\"%s\"" (name k) v))

(defn- make-link-header-element [link]
  (let [fields (map make-link-header-field (dissoc link :href))]
    (format "<%s>%s"
            (:href link)
            (if (not (empty? fields))
              (->> fields
                   (interpose " ")
                   (apply str "; "))
              ""))))

(defn make-link-header
  "Compiles a collection of links into the RFC 5988 format.
  Links are required to be maps. The :href key going into the <> part.
  eg. {:href \"/hello\" :rel \"self\" :title \"Title\"}
  -> </hello>; rel=\"self\" title=\"Title\""
  [links]
  (when-not (empty? links)
    (->> links
         (map make-link-header-element)
         (interpose ", ")
         (apply str))))

(defn- wrap-link-header-1 [handler k h]
  (fn [req]
    (let [rsp (-> req
                  (assoc k (or (k req) []))
                  handler)]
      (-> rsp
          (assoc-in [:headers h] (-> rsp k make-link-header))
          (dissoc k)))))

(defn wrap-link-header
  "Ring middleware that compiles :links and :link-templates into
  Link and Link-Template headers using octohipster.link.header/make-link-header."
  [handler]
  (-> handler
      (wrap-link-header-1 :links "Link")
      (wrap-link-header-1 :link-templates "Link-Template")))
