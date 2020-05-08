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
              :info {:title       "Cubesat Ground"
                     :description "Alpha Cubesat Ground System"}
              :tags [{:name "Debug", :description "Miscellaneous requests for debugging/sanity checks"}
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

        (GET "/img/recent" []
          :summary "Returns the most recent ttl data fully received by ground"
          :return File
          :middleware [auth/wrap-auth]
          :header-params [authorization :- s/Str]
          :produces ["image/jpeg"]
          (-> (img/get-most-recent)
              (io/input-stream)
              (ok)
              (header "Content-Type" "image/jpeg")))

        (GET "/img/recent/:id" []
          :summary "Returns the n'th most recent ttl data if it exists"
          :return File
          :middleware [auth/wrap-auth]
          :header-params [authorization :- s/Str]
          :path-params [id :- s/Int]
          :produces ["image/jpeg"]
          (try
            (-> (img/get-recent-images (inc id))
                (nth id)
                (io/input-stream)
                (ok)
                (header "Content-Type" "image/jpeg"))
            (catch Exception e
              (http/bad-request))))

        (POST "/control" []
          :return {:response uplink/CommandResponse}
          :summary "Process a command to be sent to cubesat."
          :middleware [auth/wrap-auth]
          :header-params [authorization :- s/Str]
          :body [command uplink/Command]
          (control/handle-command! command))))))