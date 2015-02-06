(ns octohipster.handlers.json
  (:require [clojure.tools.logging :as log]
            [octohipster.handlers.util :refer [defhandler make-handler-fn]]
            [octohipster.json :refer [jsonify]]))

(defhandler wrap-handler-json
  "Wraps a handler with a JSON handler."
  ["application/json"]
  (make-handler-fn jsonify))
