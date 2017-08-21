(ns reactive-playground.client
  (:require [clojure.core.async :as async]
            [clojure.string :as strings]
            [cheshire.core :as json])
  (:import (com.coreos.jetcd.options GetOption PutOption WatchOption)
           (com.coreos.jetcd.data ByteSequence KeyValue)
           (com.coreos.jetcd Client)
           (java.util.function Function Consumer)
           (java.util.concurrent CompletableFuture)
           (com.coreos.jetcd.kv GetResponse PutResponse)
           (com.coreos.jetcd.watch WatchEvent)
           (clojure.core.async.impl.channels ManyToManyChannel)))

(def POLLING_INTERVAL 100)

(defn ^Function lift-function [f]
  (reify Function
    (apply [_ arg] (f arg))))

(defn ^Consumer lift-consumer [f]
  (reify Consumer
    (accept [_ arg] (f arg))))

(defn then [^CompletableFuture future f]
  (.thenApply future (lift-function f)))

(defn accept [^CompletableFuture future f]
  (.thenAccept future (lift-consumer f)))

(defn extract-value [^GetResponse response]
  (.getKvs response))

(defn parse-key-value [^KeyValue kv]
  {(String. (.getBytes (.getKey kv)))
   (json/parse-string
     (String. (.getBytes (.getValue kv))))})

(defn parse-get-result [^GetResponse response]
  (->> response
    (extract-value)
    (map parse-key-value)
    (into {})))

(defn parse-put-result [^PutResponse response]
  (if (.hasPrevKv response)
    {:previous (parse-key-value (.getPrevKv response))}
    {}))

(defn parse-watch-event [^WatchEvent event]
  {:previous (parse-key-value (.getPrevKV event))
   :current  (parse-key-value (.getKeyValue event))
   :event    (keyword (strings/lower-case (.name (.getEventType event))))})

(defn future->chan [^CompletableFuture future]
  (let [out (async/chan)]
    (accept future (partial async/put! out))
    out))

(defn future-coll->chan [^CompletableFuture future]
  (let [out (async/chan)]
    (accept future (partial async/onto-chan out))
    out))

(defn make-client [{:keys [endpoints]}]
  (->
    (Client/builder)
    (.endpoints ^"[Ljava.lang.String;"
    (into-array String endpoints))
    (.build)))

(defn get-options [opts]
  (-> (GetOption/newBuilder)
    (.build)))

(defn put-options [opts]
  (-> (PutOption/newBuilder)
    (.build)))

(defn watch-options [opts]
  (-> (WatchOption/newBuilder)
    (.build)))

(defn attach-cleanup! [chan f]
  (add-watch (.closed ^ManyToManyChannel chan)
    (gensym "channel-cleanup")
    (fn [_ _ old-state new-state]
      (when (and (not old-state) new-state)
        (f))))
  chan)

(defn put*
  ([^Client client ^String key data]
   (put* client key data {}))
  ([^Client client ^String key data opts]
   (let [kv (.getKVClient client)]
     (->
       (.put kv
         (ByteSequence. key)
         (ByteSequence.
           (json/generate-string data))
         (put-options opts))
       (then parse-put-result)))))

(defn list*
  ([^Client client ^String key]
   (list* client key {}))
  ([^Client client ^String key opts]
   (let [kv (.getKVClient client)]
     (->
       (.get kv
         (ByteSequence. key)
         (get-options opts))
       (then parse-get-result)))))

(defn watch*
  ([^Client client ^String key]
   (watch* client key {}))
  ([^Client client ^String key opts]
   (let [watch   (.getWatchClient client)
         watcher (.watch watch
                   (ByteSequence. key)
                   (watch-options opts))
         output  (async/chan)]
     (async/go-loop [response (.listen watcher)]
       (let [events (.getEvents response)]
         (if-not (.isEmpty events)
           (async/onto-chan output
             (map parse-watch-event events) false)
           (async/<! (async/timeout POLLING_INTERVAL))))
       (recur (.listen watcher)))
     (attach-cleanup! output #(.close watcher)))))

