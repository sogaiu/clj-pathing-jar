(ns clj-pathing-jar.main
  (:require
    [clj-pathing-jar.core :as cc]))

(defn -main
  [& args]
  ;; XXX: do something about nice defaults
  (let [[pathing-jar-name classpath] args
        pathing-jar-name (or pathing-jar-name
                           "pathing.jar")
        classpath (or classpath
                    (System/getProperty "java.class.path"))]
    ;; XXX: possibly check whether pathing-jar-name already exists
    (cc/make-classpath-jar! pathing-jar-name classpath)))
