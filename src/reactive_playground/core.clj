(ns reactive-playground.core
  (:require [reactive-playground.client :as client]
            [clojure.pprint :as pprint]
            [clojure.core.async :as async]))


(def endpoints
  ["http://docker:23791"
   "http://docker:23792"
   "http://docker:23793"])

(def client
  (client/make-client
    {:endpoints endpoints}))

(defonce guard
  (async/go-loop
    [input (client/subscribe* client "/dogs")]
    (pprint/pprint (async/<! input))
    (recur input)))
