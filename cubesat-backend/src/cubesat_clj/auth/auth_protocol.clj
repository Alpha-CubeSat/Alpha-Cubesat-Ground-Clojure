(ns cubesat-clj.auth.auth-protocol
  (:require [schema.core :as s]))

(s/defschema LoginRequest
  {:username s/Str
   :password s/Str})