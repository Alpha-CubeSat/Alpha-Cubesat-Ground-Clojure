(ns cubesat-clj.telemetry.image-handler
  (:require [schema.core :as s]
            [buddy.sign.jwt :as jwt]
            [clj-time.core :as time]
            [buddy.core.keys :as keys]
            [cheshire.core :as json]
            [cubesat-clj.util.binary.byte-buffer :as buffer]
            [cubesat-clj.util.binary.binary-reader :as reader]
            [cubesat-clj.util.binary.hex-string :as hex]
            [cubesat-clj.databases.image-database :as img]
            [clojure.java.io :as io]
            [ring.util.http-response :as http]
            [cubesat-clj.util.binary.hex-string :as bin])
  (:import (org.apache.commons.io IOUtils)
           (java.io File)
           (java.util Date)))


(s/defschema ImageData
  {:name   s/Str
   :date   s/Inst
   :base64 s/Str})


(s/defschema ImageNames
  [s/Str])


(defn get-image-data
  [image-file]
  (let [b64 (-> image-file
                (io/input-stream)
                (IOUtils/toByteArray)
                (bin/bytes-to-b64))
        date (.lastModified image-file)
        name (.getName image-file)]
    {:name   name
     :date   (Date. date)
     :base64 b64}))


(defn get-image-at [idx]
  (-> (img/get-recent-images (inc idx))
      (nth idx)
      (get-image-data)))


(defn get-image-by-name [name]
  (get-image-data (img/get-image-by-name name)))


(defn get-recent-image-names [n]
  (->> (img/get-recent-images n)
       (map #(.getName %))
       vec))


(defn handle-get-image-list [count]
  (http/ok (get-recent-image-names count)))


(defn handle-image-request [name]
  (http/ok (get-image-by-name name)))

