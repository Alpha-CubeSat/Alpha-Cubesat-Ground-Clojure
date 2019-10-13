(ns cubesat-clj.auth.auth-handler
  "Handles requests related to or using security."
  (:require [cheshire.core :as json]
            [buddy.sign.jwt :as jwt]
            [buddy.auth.backends :as backends]
            [buddy.auth :as buddy]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [ring.util.http-response :as http]))


(def secret "test_secret")                                  ; TODO make configurable


(def backend (backends/jws {:secret secret}))


(defn find-user
  "Finds a user with username and correct password. TODO: For now, just authenticates everything." ; TODO make configurable
  [username password]
  {:id username})


(defn handle-login
  "Handles a login request by checking the provided credentials. Returns a signed JWT token if successful." ; TODO fail if user could not be found
  [credentials]
  (let [user (find-user (:username credentials)
                        (:password credentials))
        token (jwt/sign {:user (:id user)} secret)]
    (println "SIGNED: " (json/encode {:token token}))
    (http/ok {:token token})))


; TODO use the more idiomatic "wrap-restricted" feature in buddy once needed
(defn- wrap-check-auth
  "Middleware which checks if a request was successfully authenticated, and if not, returns an unauthorized error code
  instead of processing the request"
  [handler]
  (fn [request]
    (if (buddy/authenticated? request)
      (handler request)
      (http/unauthorized))))


(defn wrap-auth
  "Convenience function that wraps a request in middleware for authenticating, then authorizing, and
  then handling the case where it is unauthorized"
  [handler]
  (-> handler
      (wrap-check-auth)
      (wrap-authorization backend)
      (wrap-authentication backend)))