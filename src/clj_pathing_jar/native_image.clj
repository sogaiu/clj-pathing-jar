(ns clj-pathing-jar.native-image
  (:require
    [clj-pathing-jar.core :as cc]))

(defn -main
  [& args]
  ;; XXX: do something about nice defaults
  (let [[main-class classpath classes-dir pathing-jar-name] args
        _ (assert main-class "must specify a main-class")
        classes-dir (or classes-dir
                      "classes")
        classpath (or classpath
                    (System/getProperty "java.class.path"))
        pathing-jar-name (or pathing-jar-name
                           "pathing.jar")]
    ;; XXX: possibly check whether pathing-jar-name already exists
    (cc/make-native-image-pathing-jar! main-class
      classpath classes-dir pathing-jar-name)))
