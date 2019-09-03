(ns cubesat-clj.util.binary.binary-reader
  "Interface for reading binary data"
  (:require [clojure.core.match :refer [match]]))

(defprotocol binary-reader
  "Reads binary data"
  (read-int64 [source])
  (read-int32 [source])
  (read-uint32 [source])
  (read-int16 [source])
  (read-uint16 [source])
  (read-int8 [source])
  (read-uint8 [source])
  (read-float64 [source])
  (read-float32 [source])
  (read-bytes [source amount])
  (skip [source amount])
  (remaining [source])
  (reset [source]))

(defn read-type [reader type]
  "Uses a reader to read a primitive type"
  (case type
    ::int64 (read-int64 reader)
    ::int32 (read-int32 reader)
    ::uint32 (read-uint32 reader)
    ::int16 (read-int16 reader)
    ::uint16 (read-uint16 reader)
    ::int8 (read-int8 reader)
    ::uint8 (read-uint8 reader)
    ::float64 (read-float64 reader)
    ::float32 (read-float32 reader)))

(defn read-structure
  "Uses a reader to read the input structure. Supports basic types,
  buffers, and skipping bytes. Uses a data-driven interface with a vector
  for 'structure' where names and types are passed in, and returns
  a map with names and the values read from the reader.

  Ex: (read-structure reader [:item1 ::int32
                              :- 5
                              :item2 ::byte-array 22])
  reads an integer, skips 5 bytes, reads a length 22 byte array,
  and returns a map with :item1, :item2, and their corresponding values."
  [reader structure]
  (loop [remaining-structure structure
         result {}]
    (match remaining-structure
           [] result

           [:- length & more]
            (do (skip reader length) (recur more result))

           [name ::byte-array ::all-remaining]
            (assoc result name (read-bytes reader (remaining reader)))

           [name ::byte-array length & more]
            (recur more (assoc result name (read-bytes reader length)))

           [name type & more]
            (recur more (assoc result name (read-type reader type))))))
