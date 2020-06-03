(ns cubesat-clj.util.binary.hex-string
  "Utility functions for binary data encoded in strings. Code adapted from:
  https://stackoverflow.com/questions/10062967/clojures-equivalent-to-pythons-encodehex-and-decodehex"
  (:import (java.util Base64)))


(defn hexify
  "Returns hex representation of the input string"
  [s]
  (format "%x" (new BigInteger (.getBytes s))))


(defn unhexify
  "Returns a string containing a the results of decoding a hex string"
  [hex]
  (apply str
         (map
           (fn [[x y]] (char (Integer/parseInt (str x y) 16)))
           (partition 2 hex))))


(defn hex-str-to-bytes
  "Decodes a hex string into a java array of bytes"
  [hex-str]
  (into-array Byte/TYPE
              (map
                (fn [[x y]] (unchecked-byte (Integer/parseInt (str x y) 16)))
                (partition 2 hex-str))))


(defn bytes-to-b64
  [bytes]
  (.encodeToString (Base64/getEncoder) bytes))