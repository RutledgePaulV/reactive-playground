(ns reactive-playground.core-test
  (:require [clojure.test :refer :all]
            [qbits.jet.client.http :as http]
            [clojure.core.async :as a]
            [clojure.string :as strings]
            [cheshire.core :as cheshire])
  (:import (org.apache.commons.io IOUtils)))

; a hacky SSE client to receive events


(defn prepare-byte-chan [byte-chan]
  (->> byte-chan
    (a/map< #(IOUtils/toString ^bytes %))
    (a/filter< #(strings/starts-with? % "data: "))
    (a/map< #(.substring % (.length "data: ")))
    (a/map< cheshire/parse-string)))

(defn handle-response [f response]
  (let [chunked-body (:body response)]
    (if (= 200 (:status response))
      (a/go-loop [body (prepare-byte-chan chunked-body)]
        (let [chunk (a/<! body)]
          (f chunk)
          (recur body))))
    (println "Failed!")))

(defn connect [f]
  (let [client
        (http/client)
        response-chan
        (http/get client
          "http://localhost:3000/api/subscribe/customer"
          {:fold-chunked-response? false :as :bytes})]
    (handle-response f (a/<!! response-chan))))

(connect (fn [chunk] (println chunk)))
