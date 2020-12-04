(ns cubesat-clj.auth.auth-protocol
  "Data formats for requests and other communication for authentication and security"
  (:require [schema.core :as s]
            [buddy.auth.backends :as backends]
            [cubesat-clj.config :as cfg]
            [cubesat-clj.auth.user-store :as usr]
            [buddy.sign.jwt :as jwt]
            [clj-time.core :as time]))


(s/defschema AuthResult
  "Result Schema for login request."
  {:token s/Str
   :username s/Str})


(s/defschema LoginRequest
  "Format for login request."
  {:username s/Str
   :password s/Str})


(def auth-backend
  "JWS backend for token generation and usage."
  (let [config (cfg/get-config)
        secret (cfg/jws-secret config)]
    (backends/jws {:secret secret})))


(defn try-authenticate-user
  "Takes as input user credentials, and returns a token if authenticated. Returns nil on failure,
  such as non-existent user or incorrect password."
  [user pass]
  (if (usr/check-credentials user pass)
    (let [config (cfg/get-config)
          secret (cfg/jws-secret config)]
      (jwt/sign {:user user :exp (time/plus (time/now) (time/hours 24))} secret))
    nil))