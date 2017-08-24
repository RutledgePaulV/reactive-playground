(ns reactive-playground.state
  (:require [mount.core :refer [defstate]]
            [clojure.core.async :as async]
            [reactive-playground.client :as client])
  (:import (com.coreos.jetcd Client)))

(def endpoints
  ["http://docker:23791"
   "http://docker:23792"
   "http://docker:23793"])

(defstate event-source
  :start (async/chan 100)
  :stop (async/close! event-source))

(defstate events-multiplex
  :start (async/mult event-source))

(defstate etcd-client
  :start (client/make-client {:endpoints endpoints})
  :stop (when etcd-client (.close ^Client etcd-client)))
