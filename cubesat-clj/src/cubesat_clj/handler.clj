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
            [muuntaja.core :as m]
            [ring.util.http-response :as response]
            [cubesat-clj.control.control-protocol :as uplink]
            [cubesat-clj.control.control-handler :as control])
  (:import (java.io File InputStreamReader ByteArrayInputStream InputStream)
           (java.nio.charset Charset)))



(defn log-error
  "Since rockblock sends weird requests, and then doesn't tell you the responses it gets,
  we override compojure-api's error handling to log everything"
  [f type]
  (fn [^Exception e data request]
    (do (println "[DEBUG] Error: ")
        (println (.getMessage e))
        (println "DATA: " data)
        (println "REQUEST: " request)
        (f {:message (.getMessage e), :type type}))))


(defn fix-rockblock-date [handler]
  (fn [request]
    (let [time (get-in request [:body-params :transmit_time])
          formatted-time (str "20" (.replace time " " "T") "Z") ; Warning: This hack will cease to work in the year 2100
          fixed-request (assoc-in request [:body-params :transmit_time] formatted-time)]
      (println "middleware fixed time: " formatted-time)
      (handler fixed-request))))


(def app
  (api
    {:exceptions {:handlers
                  {::ex/request-parsing     (log-error response/internal-server-error :unknown)
                   ::ex/request-validation  (log-error response/internal-server-error :unknown)
                   ::ex/response-validation (log-error response/internal-server-error :unknown)
                   ::ex/default             (log-error response/internal-server-error :unknown)}}
     :swagger    {:ui   "/"
                  :spec "/swagger.json"
                  :data {:info {:title       "Cubesat Ground"
                                :description "Alpha Cubesat Ground System"}
                         :tags [{:name "API", :description "Satellite control/data API"}
                                {:name "Telemetry", :description "Rockblock web services telemetry endpoint"}]}}}

    (context "/telemetry" []
      :tags ["Telemetry"]

      (POST "/echo" []
        :consumes ["text/plain"]
        :body [body s/Str]
        (do (println "Echo: " body)
            (ok body)))

      (GET "/ping" []
        :return s/Str
        :summary "Test the API"
        (ok "pong"))

      (POST "/rockblock" []
        :middleware [fix-rockblock-date]
        :return nil
        :summary "Receive data from rockblock web services"
        :body [report downlink/RockblockReport]
        (telemetry/handle-report! report)))

    (context "/api" []
      :tags ["API"]

      (GET "/img/recent" []
        :summary "Returns the most recent ttl data fully received by ground"
        :return File
        :produces ["image/jpeg"]
        (-> (img/get-most-recent)
            (io/input-stream)
            (ok)
            (header "Content-Type" "image/jpeg")))

      (POST "/control" []
        :return {:result s/Str}
        :summary "Process a command to be sent to cubesat."
        :body [command uplink/Command]
        (control/handle-command! command)))))