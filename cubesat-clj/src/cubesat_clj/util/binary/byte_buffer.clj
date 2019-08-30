(ns cubesat-clj.util.binary.byte-buffer
  "Java nio ByteBuffer implementation for reading binary data"
  (:require [cubesat-clj.util.binary.binary-reader :refer [binary-reader]])
  (:import (java.nio ByteBuffer)))

(extend-type ByteBuffer
  binary-reader

  (read-int32 [buffer]
    (.getInt buffer))

  (read-float32 [buffer]
    (.getFloat buffer))

  (read-bytes [buffer length]
    (let [bytes (byte-array length)]
      (.get buffer bytes 0 length)
      bytes))

  (skip [buffer length]
    (.position buffer (+ length (.position buffer))))

  (reset [buffer]
    (.rewind buffer)))

(defn from-byte-array [bytes]
  (ByteBuffer/wrap bytes))

(defn set-endianness [buffer endianness]
  (do (.order endianness)
      buffer))