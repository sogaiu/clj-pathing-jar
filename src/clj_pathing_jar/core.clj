(ns clj-pathing-jar.core
  (:require
    [clojure.string :as string]
    [clojure.java.io :as jio])
  (:import
    [java.security MessageDigest]
    [java.nio.file Files Paths Path]
    [java.util.jar JarOutputStream Attributes$Name Manifest]
    [java.io File]))

;; basically an adaptation of tim gilbert's lein-classpath-jar:
;;   https://github.com/timgilbert/lein-classpath-jar

;; overall idea here based on https://github.com/nickgieschen/lein-extend-cp/

;; Cribbing from:
;;   https://github.com/tebeka/clj-digest/blob/master/src/digest.clj here
(defn md5
  "Produce an MD5 checksum similar to `B80AC5C8E229CFFD374E8A74168225CF`"
  [^String string]
  (let [digest (doto (MessageDigest/getInstance "MD5")
                 (.update (.getBytes string "UTF-8")))
        value  (BigInteger. 1 (.digest digest))]
    (format (str "%0" (* 2 (.getDigestLength digest)) "X") value)))

(defn ^Path jar-path-for-classpath [classpath]
  (Paths/get (System/getProperty "user.dir")
             (into-array String [".cpcache"
                                 (str (md5 classpath) ".jar")])))

(defn- absolute-uri [path]
  (some-> path jio/file .getAbsoluteFile .toURI .toString))

(defn- manifest-classpath
  "Translate the classpath into a format suitable for a jar manifest, by
  changing all of the paths to file:// URIs and separating the list by
  single spaces."
  [cp-list]
  (->> cp-list (map absolute-uri) (string/join " ")))

(defn create-cpcache-dir!
  "Create the .cpcache directory (in the current directory, assumed to
  be the project root) if it doesn't exist."
  []
  (Files/createDirectories
    (Paths/get (System/getProperty "user.dir")
      (into-array [".cpcache"]))
    (into-array java.nio.file.attribute.FileAttribute [])))

(defn create-classpath-jar!
  "Create a \"pathing jar\" - a jar file containing only a classpath in
  its manifest, useful in systems where command-line arguments have a
  limited length."
  [^Path jar-path cp-list]
  (create-cpcache-dir!)
  (let [^Manifest manifest (Manifest.)]
   (doto (.getMainAttributes manifest)
    (.put Attributes$Name/CLASS_PATH (manifest-classpath cp-list))
    (.put Attributes$Name/MANIFEST_VERSION "1.0"))
   (doto (JarOutputStream.
           (Files/newOutputStream jar-path
             (into-array java.nio.file.OpenOption [])) manifest)
    (.setLevel JarOutputStream/STORED)
    (.flush)
    (.close))
   (.toString jar-path)))

(defn cache-classpath-jar
  "Given a classpath string, take an MD5 hash of the classpath and look
  for a file `.cpcache/<hash>.jar` directory in the current working
  directory. If the file doesn't exist, create it. Either way, return
  the pathing jar as the sole element of the classpath."
  [classpath]
  (let [cp-list (string/split classpath (re-pattern File/pathSeparator))
        jar-path (jar-path-for-classpath classpath)
        jar-str (-> jar-path .toAbsolutePath .toString)]
    (if-not (Files/exists jar-path (into-array java.nio.file.LinkOption []))
      (do
        (create-classpath-jar! jar-path cp-list)
        (println "Created new pathing jar: " jar-str))
      (println "Using existing pathing jar for classpath: " jar-str))
    [jar-str]))
