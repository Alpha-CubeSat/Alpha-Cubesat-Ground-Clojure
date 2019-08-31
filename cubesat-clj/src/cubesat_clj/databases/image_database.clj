(ns cubesat-clj.databases.image-database
  "For storing images on the local filesystem, and assembling them
   out of binary fragments"
  (:require [cubesat-clj.config :as cfg])
  (:import (java.io File FileOutputStream FileInputStream)
           (java.nio.file Path Files OpenOption StandardOpenOption)
           (java.nio.file.attribute FileAttribute)))

(defn- get-root-path
  "Gets the root directory of the image database as given in the config file"
  []
  (-> (cfg/get-config) :database :image :root))

(defn save-fragment
  "saves an image fragment. Creates one if it doesn't exist using the following policy:
  - new file is created under the directory with the serial number of the image
  - the name of the file will be the fragment number with the .csfrag extension
  Example:
    If fragment 7 of image 23 is saved,
    then it will be saved as '7.csfrag' in the directory named '23'.
  Note: binary-fragment-data accepts a JAVA BYTE ARRAY, byte[], not a ByteBuffer or clojure vector"
  [image-sn fragment-number binary-fragment-data]
  (let [db-root-dir (get-root-path)
        frag-file (File. (str db-root-dir "/" image-sn "/" fragment-number ".csfrag"))]
    (-> frag-file .getParentFile .mkdirs)                   ; Make the parent directories if not present
    (println (.getCanonicalPath frag-file))
    (doto (FileOutputStream. frag-file)
      (.write binary-fragment-data 0 (alength binary-fragment-data))
      (.flush)
      (.close))))

(defn- sort-fragment-files
  "Sorts fragment files by name. They are numerically named,
  but '2.csfrag' should come before '10.csfrag'"
  [files]
  (sort-by #(-> % .getName (.replace ".csfrag" "") Integer/parseInt) files))

(defn try-save-image
  "Tries to assemble an image out of fragments. If there are enough, saves the image
   to the directory 'img' as a jpeg file with the serial number as a name. Returns nil
   otherwise.
   Example:
     If image 23 is complete and try-save-image is called, the completed image will
     be saved as '23.png' under 'img', and assembled out of the fragment files in the directory '23'"
  [image-sn total-fragment-number]
  (let [db-root-dir (get-root-path)
        fragment-dir (str db-root-dir "/" image-sn)
        fragment-files (rest (->> fragment-dir clojure.java.io/file file-seq))
        num-fragments (count fragment-files)
        img-dir (str db-root-dir "/img")
        img-file-path (str img-dir "/" image-sn ".jpeg")]
    (.mkdirs (File. img-dir))                               ; make img dir if it doesnt exist
    (when (= num-fragments total-fragment-number)
      (let [sorted-fragments (sort-fragment-files fragment-files)
            image-file (FileOutputStream. img-file-path)]
        (dorun (for [^File fragment sorted-fragments]
                 (.transferTo (FileInputStream. fragment) image-file)))
        (doto image-file (.flush) (.close))))))