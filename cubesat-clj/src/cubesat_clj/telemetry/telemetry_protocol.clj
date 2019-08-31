(ns cubesat-clj.telemetry.telemetry-protocol
  "Specifies/parses formats used by the RockBlock API and cubesat.
  These formats are described in the RockBlock web services docs at:
  https://docs.rock7.com/reference#push-api
  And for the satellite in the Google Drive for Alpha"
  (:require [schema.core :as s]
            [buddy.sign.jwt :as jwt]
            [clj-time.core :as time]
            [buddy.core.keys :as keys]
            [cheshire.core :as json]))

(s/defschema RockblockReport
  "A report submitted by the rockblock API. Contains main fields with satellite
  data and information, along with optionally sent fields, and some unknown ones
  that may not be consistent with the rockblock docs.

  The 'message' field contains the hex-encoded binary data sent by the satellite."
  {:id                                 s/Str
   :transport                          s/Str
   ;:imei                               s/Str
   :device_type                        s/Str
   :serial                             s/Int
   :momsn                              s/Int
   :transmit_time                      s/Inst
   ;:message                            s/Str
   :at                                 s/Inst
   :JWT                                s/Str
   (s/optional-key :iridium_longitude) s/Num
   (s/optional-key :iridium_latitude)  s/Num
   (s/optional-key :cep)               s/Int
   (s/optional-key :trigger)           s/Str
   (s/optional-key :source)            s/Str
   (s/optional-key :lat)               s/Num
   (s/optional-key :lon)               s/Num
   (s/optional-key :sog)               s/Num
   (s/optional-key :cog)               s/Num
   (s/optional-key :alt)               s/Num
   (s/optional-key :temp)              s/Num
   (s/optional-key :battery)           s/Num
   (s/optional-key :power)             s/Bool
   (s/optional-key :ack_request)       s/Int
   (s/optional-key :message_ack)       s/Int
   (s/optional-key :alert)             s/Bool
   (s/optional-key :waypoint)          s/Str
   (s/optional-key :appMessageAddress) [s/Str]
   (s/optional-key :appMessageContent) s/Str
   (s/optional-key :beacons)           [s/Str]
   s/Any s/Any})

(def rockblock-web-pk
  "Public key provided for JWT verification by rockblock web services documentation"
  (keys/str->public-key
    "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlaWAVJfNWC4XfnRx96p9cztBcdQV6l8aKmzAlZdpEcQR6MSPzlgvihaUHNJgKm8t5ShR3jcDXIOI7er30cIN4/9aVFMe0LWZClUGgCSLc3rrMD4FzgOJ4ibD8scVyER/sirRzf5/dswJedEiMte1ElMQy2M6IWBACry9u12kIqG0HrhaQOzc6Tr8pHUWTKft3xwGpxCkV+K1N+9HCKFccbwb8okRP6FFAMm5sBbw4yAu39IVvcSL43Tucaa79FzOmfGs5mMvQfvO1ua7cOLKfAwkhxEjirC0/RYX7Wio5yL6jmykAHJqFG2HT0uyjjrQWMtoGgwv9cIcI7xbsDX6owIDAQAB\n-----END PUBLIC KEY-----"))

(defn verify-rockblock-request [rockblock-report]
  "Uses jwt to verify data sent by rockblock web services. Returns the data if valid,
  nil if invalid/corrupt"
  (try (let [jwt (:JWT rockblock-report)
             unsigned-data (jwt/unsign jwt rockblock-web-pk {:alg :rs256})]
         (clojure.walk/keywordize-keys unsigned-data))      ;Have to convert the decoded json to edn, even though original, unencoded, request was in edn
       (catch Exception e (do (.printStackTrace e) nil))))

;(defn -main []
;  (do
;    (let [data (json/decode "{\n  \"averageCog\": 0,\n  \"iridium_latitude\": 42.0588,\n  \"device_type\": \"TIGERSHARK\",\n  \"lon\": 19.09065,\n  \"sog\": 0.0,\n  \"source\": \"GPS\",\n  \"battery\": 82,\n  \"cep\": 5,\n  \"momsn\": 337,\n  \"id\": \"rLzVjdQqPgkanllZloYlnpRyKbJZONAG\",\n  \"power\": false,\n  \"transmit_time\": \"2019-01-31T14:00:13Z\",\n  \"lat\": 42.09897,\n  \"txAt\": \"2019-01-31T14:00:13Z\",\n  \"pdop\": 1.25,\n  \"temp\": 8.0,\n  \"alt\": 6,\n  \"transport\": \"IRIDIUM\",\n  \"trigger\": \"BURST\",\n  \"iridium_longitude\": 19.0618,\n  \"averageSog\": 0.0,\n  \"at\": \"2019-01-31T14:00:00Z\",\n  \"serial\": 21341,\n  \"cog\": 0,\n  \"JWT\": \"eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJSb2NrIDciLCJpYXQiOjE1NDg5NTIzMzIsImFsdCI6IjYiLCJhdCI6IjIwMTktMDEtMzFUMTQ6MDA6MDBaIiwiYXZlcmFnZUNvZyI6IjAiLCJhdmVyYWdlU29nIjoiMC4wIiwiYmF0dGVyeSI6IjgyIiwiY2VwIjoiNSIsImNvZyI6IjAiLCJkZXZpY2VfdHlwZSI6IlRJR0VSU0hBUksiLCJpZCI6InJMelZqZFFxUGdrYW5sbFpsb1lsbnBSeUtiSlpPTkFHIiwiaXJpZGl1bV9sYXRpdHVkZSI6IjQyLjA1ODgiLCJpcmlkaXVtX2xvbmdpdHVkZSI6IjE5LjA2MTgiLCJsYXQiOiI0Mi4wOTg5NyIsImxvbiI6IjE5LjA5MDY1IiwibW9tc24iOiIzMzciLCJwZG9wIjoiMS4yNSIsInBvd2VyIjoiZmFsc2UiLCJzZXJpYWwiOiIyMTM0MSIsInNvZyI6IjAuMCIsInNvdXJjZSI6IkdQUyIsInRlbXAiOiI4LjAiLCJ0cmFuc21pdF90aW1lIjoiMjAxOS0wMS0zMVQxNDowMDoxM1oiLCJ0cmFuc3BvcnQiOiJJUklESVVNIiwidHJpZ2dlciI6IkJVUlNUIiwidHhBdCI6IjIwMTktMDEtMzFUMTQ6MDA6MTNaIn0.fxLTO4KCy-94rxVhVWrOWdXNgdWR9FLqBlBjtO2uJQlo_njbIOuiU_M8CAv4f1lon6IbbTPen4mRiSIR26S8gn3TUvPdIzzq769bVQBGNiywmwDXZbCJC3-gFi07vcvpyeXPnaEegS1M-Acd-bsC9ORzGeGTSRz5Mp9uajCJ_BSUM7ljMiZajZ6WPoVTgPwTrJ9BdUuz78qipdEQRUW1qdoubkl21SyMYRonB39CMXkA4MTrbITM1g_3viGBVglijIyVh2fumLErYP5SvwfVxNXDSuC5LFHqIszojc3gf5xwuR-fCt4CbzL_I7lOCuct3_kRiYGbUSDpU5Ytp6e1wA\"\n}")
;          jwt (data "JWT")
;          unsigned (jwt/unsign jwt rockblock-web-pk {:alg :rs256})]
;      (println data)
;      (println jwt)
;      (println unsigned))))