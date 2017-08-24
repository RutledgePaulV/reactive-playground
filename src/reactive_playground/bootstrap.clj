(ns reactive-playground.bootstrap
  (:require [ring.core.protocols :refer [StreamableResponseBody]]
            [taoensso.timbre :as logger]
            [clojure.core.async :as async]
            [selmer.parser :as selmer]
            [clojure.java.io :as io]
            [mount.core :as mount])
  (:import (java.io IOException OutputStream)
           (clojure.core.async.impl.channels ManyToManyChannel)))

(extend-type ManyToManyChannel
  StreamableResponseBody
  (write-body-to-stream [channel _ ^OutputStream output-stream]
    (async/go
      (try
        (loop []
          (when-let [msg (async/<! channel)]
            (do
              (if-not (= :flush msg)
                (doto output-stream
                  (.write ^bytes msg)
                  (.flush))
                (.flush output-stream))
              (recur))))
        (catch IOException e
          (async/close! channel))
        (catch Exception e
          (logger/error "Encountered error sending server event." e))))))

(selmer/cache-off!)
(alter-var-root #'selmer.util/*filter-open* (constantly \[))
(alter-var-root #'selmer.util/*filter-close* (constantly \]))
(selmer/set-resource-path! (io/resource "templates"))
(mount/start)