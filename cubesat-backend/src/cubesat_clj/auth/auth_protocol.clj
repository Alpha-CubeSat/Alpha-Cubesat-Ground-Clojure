(ns cubesat-clj.auth.auth-protocol
  "Data formats for requests and other communication for authentication and security"
  (:require [schema.core :as s]))

(s/defschema LoginRequest
  {:username s/Str
   :password s/Str})