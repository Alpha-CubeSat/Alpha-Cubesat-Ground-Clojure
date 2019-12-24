(ns cubesat-clj.config
  "Configuration file for entire ground system"
  (:require [aero.core :as aero]
            [schema.core :as s]))

(def config-file
  "Location of the configuration file"
  "config.edn")

(s/defschema AlphaConfig
  "Format for configuration; includes 'sub-configurations'
  for all modules such as telemetry and databases"
  {:docs {:enabled? s/Bool
          (s/optional-key :base-path) s/Str}
   :telemetry {:elasticsearch-indices {:rockblock s/Str
                                       :cubesat   s/Str}}
   :database  {:elasticsearch {:host        s/Str
                               :port        s/Int
                               :conn-config {(s/optional-key :basic-auth)   [s/Str] ; ["user" "pass"]
                                             (s/optional-key :conn-timeout) s/Int ; timeout in millis
                                             :content-type                  (s/enum :json)}} ; Elastich requires json but doesnt use it by default
               :image         {:root s/Str}}
   :control {:rockblock {:imei s/Str
                         :basic-auth [s/Str]}}}) ; ["user" "pass"]

(def config
  "Atom that stores the config data"
  (atom nil))

(defn- load-config!
  "Loads a config from telemetry-config-file, and uses Schema to validate it."
  []
  (->> config-file
       (aero/read-config)
       (s/validate AlphaConfig)
       (reset! config)))

(defn get-config
  "Retrieves the config. If not loaded, config is loaded and then retrieved."
  []
  (if @config
    @config
    (do (load-config!) @config)))


(defn docs-enabled?
  "Returns whether API documentation generation/hosting is enabled."
  [config]
  (get-in config [:docs :enabled]))


(defn docs-base-path
  "Returns configured base path for documentation location. If not set, returns the default route of \"/\"."
  [config]
  (if-let [path (get-in config [:docs :base-path])]
    path
    "/"))


(defn rockblock-db-index
  "Returns the configured ElasticSearch index base name for RockBlock data."
  [config]
  (get-in config [:telemetry :elasticsearch-indices :rockblock]))


(defn cubesat-db-index
  "Returns the configured ElasticSearch index base name for Cubesat data."
  [config]
  (get-in config [:telemetry :elasticsearch-indices :cubesat]))


(defn elasticsearch-endpoint
  "Returns ElasticSearch connection information."
  [config]
  (let [es-conf (get-in config [:database :elastisearch])
        {:keys [host port] :as endpoint} es-conf]
    endpoint))


(defn elasticsearch-config
  "Returns the ElasticSearch connection configuration."
  [config]
  (get-in config [:database :elasticsearch :conn-config]))


(defn image-root-dir
  "Returns root directory path for local image store."
  [config]
  (get-in config [:database :image :root]))


(defn rockblock-imei
  "Returns the imei of the RockBlock."
  [config]
  (get-in config [:control :rockblock :imei]))


(defn rockblock-credentials
  "Returns credentials for RockBlock account."
  [config]
  (get-in config [:contorl :rockblock :basic-auth]))