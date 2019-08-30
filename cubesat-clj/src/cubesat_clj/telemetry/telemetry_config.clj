(ns cubesat-clj.telemetry.telemetry-config
  "Configuration for module that processes telemetry data"
  (:require [aero.core :as aero]
            [schema.core :as s]))

(def telemetry-config-file "telemetry_config.edn")

(s/defschema TelemetryConfig
  "Format for telemetry module configuration. Requires a name
  for an elasticsearch index where rockblock data is stored"
  {:elasticsearch-indices {:rockblock s/Str}})

(def config (atom nil))

(defn load-config!
  "Loads a config from telemetry-config-file, and uses Schema to validate it."
  []
  (->> telemetry-config-file
       (aero/read-config)
       (s/validate TelemetryConfig)
       (reset! config)))

(defn get-config
  "Retrieves the config. If not loaded, config is loaded and then retrieved."
  []
  (if @config
    @config
    (load-config!)))