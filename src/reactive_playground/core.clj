(ns reactive-playground.core
  (:require [reactive-playground.client :as client]
            [clojure.core.async :as async]))


(def endpoints
  ["http://docker:23791"
   "http://docker:23792"
   "http://docker:23793"])

(def client
  (client/make-client
    {:endpoints endpoints}))

(let [watch (client/watch* client "dogs")]
  (println watch)
  (async/go-loop [input watch]
    (println (async/<! input))
    (recur watch)))

(client/put* client "dogs" {:stuff "things"})
