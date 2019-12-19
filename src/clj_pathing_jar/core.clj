(ns clj-pathing-jar.core
  (:require
    [clojure.string :as cs]
    [clojure.java.io :as cji])
  (:import
    [java.io File]
    [java.nio.file Files Path Paths]
    [java.util.jar Attributes$Name JarOutputStream Manifest]))

;; TODO: figure out a way to resolve relative paths based on something
;;       other than user.dir?

;; based on tim gilbert's lein-classpath-jar:
;;   https://github.com/timgilbert/lein-classpath-jar

(defn- absolute-uri [path]
  (some-> path
    cji/file
    .getAbsoluteFile
    .toURI
    .toString))

(defn- manifest-classpath
  "Translate the classpath into a format suitable for a jar manifest, by
  changing all of the paths to file: URIs and separating the list by
  single spaces."
  [cp-list]
  (->> cp-list
    (map absolute-uri)
    (cs/join " ")))

(defn make-classpath-jar!
  "Make a \"pathing jar\" - a jar file containing only a classpath in
  its manifest, useful in systems where command-line arguments have a
  limited length."
  [jar-file-path classpath]
  (let [cp-list (cs/split classpath
                  (re-pattern File/pathSeparator))
        ^Path jar-path (Paths/get jar-file-path
                         (into-array String []))
        ^Manifest manifest (Manifest.)]
   (doto (.getMainAttributes manifest)
     (.put Attributes$Name/CLASS_PATH
       (manifest-classpath cp-list))
     (.put Attributes$Name/MANIFEST_VERSION
       "1.0"))
   (doto (JarOutputStream.
           (Files/newOutputStream jar-path
             (into-array java.nio.file.OpenOption [])) manifest)
     (.setLevel JarOutputStream/STORED)
     (.flush)
     (.close))
   (.toString jar-path)))

(defn- simple-manifest-classpath
  "Translate the classpath into a format suitable for a jar manifest, by
   separating the list by single spaces."
  [cp-list]
  (cs/join " " cp-list))

(defn make-native-image-pathing-jar!
  "Make a \"pathing jar\" for use with native-image - a jar file containing
  a classpath in its manifest along with a main-class."
  [main-class classpath classes-dir jar-file-path]
  (let [cp-list (cons classes-dir
                  (cs/split classpath
                    (re-pattern File/pathSeparator)))
        ^Path jar-path (Paths/get jar-file-path
                         (into-array String []))
        ^Manifest manifest (Manifest.)]
   (doto (.getMainAttributes manifest)
     (.put Attributes$Name/MAIN_CLASS
       main-class)
     (.put Attributes$Name/CLASS_PATH
       (simple-manifest-classpath cp-list))
     (.put Attributes$Name/MANIFEST_VERSION
       "1.0"))
   (doto (JarOutputStream.
           (Files/newOutputStream jar-path
             (into-array java.nio.file.OpenOption [])) manifest)
     (.setLevel JarOutputStream/STORED)
     (.flush)
     (.close))
   (.toString jar-path)))
