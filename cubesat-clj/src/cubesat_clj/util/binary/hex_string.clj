(ns cubesat-clj.util.binary.hex-string
  "Utility functions for binary data encoded in strings. Code adapted from:
  https://stackoverflow.com/questions/10062967/clojures-equivalent-to-pythons-encodehex-and-decodehex")

(defn unhexify [hex]
  "Returns a string containing a the results of decoding a hex string"
  (apply str
         (map
           (fn [[x y]] (char (Integer/parseInt (str x y) 16)))
           (partition 2 hex))))

(defn hex-str-to-bytes [hex-str]
  "Decodes a hex string into a java array of bytes"
  (into-array Byte/TYPE
              (map
                (fn [[x y]] (unchecked-byte (Integer/parseInt (str x y) 16)))
                (partition 2 hex-str))))