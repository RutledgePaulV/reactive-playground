(ns reactive-playground.handlers
  (:require [clojure.core.async :as async]
            [reactive-playground.state :as state]
            [reactive-playground.client :as client]))


(defmulti handle-transaction
  (fn [resource id {:strs [action payload]}]
    [(keyword resource) (keyword action)]))


(defn transact! [request resource id params]
  (async/<!!
    (client/put*
      state/etcd-client
      (str "/" resource "/" id)
      params)))

(defn subscribe [request resource]
  (client/subscribe*
    state/etcd-client
    (str "/" resource)))