(ns cubesat-clj.databases.elasticsearch
  "Functions for storing data in ElasticSearch"
  (:require [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.document :as esd])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))

;; Some default config stuff until a database config file is needed
(def endpoint "http://127.0.0.1:9200")
(def conn-config {:conn-timeout 5000})

(defn literal-index-strategy
  "Returns the input name"
  [name]
  name)

(defn daily-index-strategy
  "Appends the current year, month, and day to the supplied index name
  to break up an index into a daily index pattern."
  [name]
  (let [today (.format (SimpleDateFormat. "yyyy.MM.dd") (Date.))]
    (str name "-" today)))

(defn index!
  "Indexes the supplied document into elasticsearch, using the
  naming strategy to create indices using the given index name.

  Note that document types will soon be deprecated in Elasticsearch.
  So by default we give everything the '_doc' type for an easy transition
  to new Elasticsearch versions. Then, to differentiate different data,
  use separate indices. This is now the approach recommended by Elastic."
  [index-base-name naming-strategy content]
  (let [conn (esr/connect endpoint conn-config)]
    (esd/create conn (naming-strategy index-base-name) "_doc" content)))