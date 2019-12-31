(ns cubesat-clj.auth.auth-protocol
  "Data formats for requests and other communication for authentication and security"
  (:require [schema.core :as s]
            [buddy.auth.backends :as backends]
            [cubesat-clj.config :as cfg]
            [buddy.sign.jwt :as jwt]))


(s/defschema LoginRequest
  "Format for login request."
  {:username s/Str
   :password s/Str})


(def auth-backend
  "JWS backend for token generation and usage."
  (let [config (cfg/get-config)
        secret (cfg/jws-secret config)]
    (backends/jws {:secret secret})))


;;TODO actually check the password
(defn authenticate-user
  "Takes as input user credentials, and returns a token if authencated. Returns nil on failure,
  such as non-existent user or incorrect password."
  [user pass]
  (let [config (cfg/get-config)
        secret (cfg/jws-secret config)
        token (jwt/sign {:user user} secret)]
    token))