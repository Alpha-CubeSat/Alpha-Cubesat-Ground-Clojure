(ns cubesat-clj.config
  "Configuration file for entire ground system"
  (:require [aero.core :as aero]
            [schema.core :as s]))

(def config-file
  "Location of the configuration file"
  "config.edn")

(s/defschema AlphaConfig
  "Format for configuration; includes 'sub-configurations'
  for all modules such as telemetry"
  {:telemetry {:elasticsearch-indices {:rockblock s/Str
                                       :cubesat   s/Str}}
   :database  {:elasticsearch {:host        s/Str
                               :port        s/Int
                               :conn-config s/Any}
               :image         {:root s/Str}}})

(def config
  "Atem that stores the config data"
  (atom nil))

(defn load-config!
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