(ns cubesat-clj.util.binary.byte-buffer
  "Java nio ByteBuffer implementation for reading binary data"
  (:require [cubesat-clj.util.binary.binary-reader :refer [binary-reader]])
  (:import (java.nio ByteBuffer)))

(extend-type ByteBuffer
  binary-reader

  (read-uint32 [buffer]
    (bit-and (.getInt buffer) 0x00000000FFFFFFFF))

  (read-int32 [buffer]
    (.getInt buffer))

  (read-uint16 [buffer]
    (bit-and (.getShort buffer) 0x0000FFFF))

  (read-int16 [buffer]
    (.getShort buffer))

  (read-uint8 [buffer]
    (bit-and (.get buffer) 0x000000FF))

  (read-int8 [buffer]
    (.get buffer))

  (read-float32 [buffer]
    (.getFloat buffer))

  (read-bytes [buffer length]
    (let [bytes (byte-array length)]
      (.get buffer bytes 0 length)
      bytes))

  (skip [buffer length]
    (.position buffer (+ length (.position buffer))))

  (remaining [buffer]
    (.remaining buffer))

  (reset [buffer]
    (.rewind buffer)))

(defn from-byte-array [bytes]
  (ByteBuffer/wrap bytes))

(defn set-endianness [buffer endianness]
  (do (.order endianness)
      buffer))