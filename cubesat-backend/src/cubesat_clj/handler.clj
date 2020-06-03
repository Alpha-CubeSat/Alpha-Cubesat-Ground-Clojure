(ns cubesat-clj.handler
  "Main ring app handler that routes all requests. Generates API
  docs with Swagger UI, available at root url."
  (:require [compojure.api.sweet :refer :all]
            [compojure.api.exception :as ex]
            [ring.util.http-response :refer :all]
            [cubesat-clj.telemetry.telemetry-protocol :as downlink]
            [cubesat-clj.telemetry.telemetry-handler :as telemetry]
            [cubesat-clj.databases.image-database :as img]
            [schema.core :as s]
            [clojure.java.io :as io]
            [ring.util.http-response :as response]
            [cubesat-clj.control.control-protocol :as uplink]
            [cubesat-clj.control.control-handler :as control]
            [cubesat-clj.auth.auth-handler :as auth]
            [cubesat-clj.auth.auth-protocol :as auth-data]
            [cubesat-clj.config :as cfg]
            [cubesat-clj.telemetry.image-handler :as images]
            [ring.util.http-response :as http])
  (:import (java.io File InputStreamReader ByteArrayInputStream InputStream)
           (java.nio.charset Charset)))


(defn- log-error
  "Since rockblock sends weird requests, and then doesn't tell you the responses it gets,
  we override compojure-api's error handling to log everything"
  [f type]
  (fn [^Exception e data request]
    (do (println "[DEBUG] Error: ")
        (println (.getMessage e))
        (.printStackTrace e)
        ;(println "DATA: " data)
        ;(println "REQUEST: " request)
        (f {:message (.getMessage e), :type type}))))

(defn- make-docs []
  "If configured to do so, returns a map containing swagger UI spec"
  (let [config (cfg/get-config)
        enabled (cfg/docs-enabled? config)
        base (cfg/docs-base-path config)]
    (if (not enabled)
      {}
      {:ui   "/api"
       :spec "/api/swagger.json"
       :data {:basePath base
              :info     {:title       "Cubesat Ground"
                         :description "Alpha Cubesat Ground System"}
              :tags     [{:name "Debug", :description "Miscellaneous requests for debugging/sanity checks"}
                         {:name "Auth", :description "Login endpoint"}
                         {:name "API", :description "Satellite control/data API"}
                         {:name "Telemetry", :description "Rockblock web services telemetry endpoint"}]}})))

(def app
  (api
    {:exceptions {:handlers
                  {::ex/request-parsing     (log-error response/internal-server-error :unknown)
                   ::ex/request-validation  (log-error response/internal-server-error :unknown)
                   ::ex/response-validation (log-error response/internal-server-error :unknown)
                   ::ex/default             (log-error response/internal-server-error :unknown)}}
     :swagger    (make-docs)}

    (context "/api" []


      (context "/debug" []
        :tags ["Debug"]

        (GET "/ping" []
          :return s/Str
          :summary "Test the API"
          (do (println "Got a ping")
              (ok "pong")))

        (POST "/echo" []
          :consumes ["text/plain"]
          :body [body s/Str]
          (do (println "Echo: " body)
              (ok body))))


      (context "/auth" []
        :tags ["Auth"]

        (POST "/login" []
          :return {:token s/Str}
          :summary "Authenticate to get a token for api access"
          :body [credentials auth-data/LoginRequest]
          (auth/handle-login credentials)))


      (context "/rockblock" []
        :tags ["RockBlock"]

        (POST "/telemetry" []
          :return nil
          :summary "Receive data from rockblock web services"
          :middleware [telemetry/verify-rockblock-data
                       telemetry/fix-rockblock-date]
          :body [report downlink/RockblockReport]
          (telemetry/handle-report! report)))


      (context "/cubesat" []
        :tags ["Cubesat"]

        (GET "/img/:name" []
          :summary "Returns the ttl file with the given name if it exists"
          :return images/ImageData
          :middleware [auth/wrap-auth]
          :header-params [authorization :- s/Str]
          :path-params [name :- s/Str]
          (images/handle-image-request name))

        (GET "/img/recent/list" []
          :summary "Returns a list of names of recently received ttl files"
          :return images/ImageNames
          :middleware [auth/wrap-auth]
          :header-params [authorization :- s/Str]
          :query-params [count :- s/Int]
          (images/handle-get-image-list count))

        (POST "/control" []
          :return {:response uplink/CommandResponse}
          :summary "Process a command to be sent to cubesat."
          :middleware [auth/wrap-auth]
          :header-params [authorization :- s/Str]
          :body [command uplink/Command]
          (control/handle-command! command))))))