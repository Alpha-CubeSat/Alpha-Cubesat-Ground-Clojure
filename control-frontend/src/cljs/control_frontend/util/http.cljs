(ns control-frontend.util.http)

(defn token [payload]
  (str "Token " payload))