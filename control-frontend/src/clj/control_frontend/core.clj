(ns control-frontend.core
  (:require [control-frontend.handler :refer [handler]]
            [config.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

(defn -main [& args]
  (let [port (Integer/parseInt (or (env :port) "167.71.23.99:8000"))]
    (run-jetty handler {:port port :join? false})))
