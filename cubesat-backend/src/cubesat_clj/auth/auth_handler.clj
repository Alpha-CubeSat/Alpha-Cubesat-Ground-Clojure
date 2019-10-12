(ns cubesat-clj.auth.auth-handler
  (:require [cheshire.core :as json]
            [buddy.sign.jwt :as jwt]
            [buddy.auth.backends :as backends]
            [buddy.auth :as buddy]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [ring.util.http-response :as http]))


(def secret "test_secret")                                  ; TODO make configurable


(def backend (backends/jws {:secret secret}))


(defn find-user
  "Finds a user with username and correct password. TODO: For now, just authenticates everything."
  [username password]
  {:id username})


(defn handle-login
  "Handles a login request by checking the provided credentials "
  [credentials]
  (let [user (find-user (:username credentials)
                        (:password credentials))
        token (jwt/sign {:user (:id user)} secret)]
    (println "SIGNED: " (json/encode {:token token}))
    (http/ok {:token token})))


; TODO use the more idiomatic "wrap-restricted" feature in buddy once needed
(defn- wrap-check-auth [handler]
  (fn [request]
    (if (buddy/authenticated? request)
      (handler request)
      (http/unauthorized))))


(defn wrap-auth [handler]
  (-> handler
      (wrap-authentication backend)
      (wrap-authorization backend)
      (wrap-check-auth)))