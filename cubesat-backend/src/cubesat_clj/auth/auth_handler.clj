(ns cubesat-clj.auth.auth-handler
  "Handles requests related to or using security."
  (:require [buddy.auth :as buddy]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [ring.util.http-response :as http]
            [cubesat-clj.auth.auth-protocol :as auth]))

(defn handle-login
  "Handles a login request by checking the provided credentials. Returns a signed JWT token if successful." ; TODO fail if user could not be found
  [credentials]
  (let [{:keys [username password]} credentials
        token (auth/try-authenticate-user username password)]
    (if token
      (http/ok {:token token
                :username username})
      (http/bad-request {:error "Invalid user or incorrect password"}))))


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
  "Convenience function that wraps a request in middleware for authenticating, authorizing, and
  then handling the case where it is unauthorized"
  [handler]
  (-> handler
      (wrap-check-auth)
      (wrap-authorization auth/auth-backend)
      (wrap-authentication auth/auth-backend)))