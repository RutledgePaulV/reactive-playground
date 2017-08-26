(ns reactive-playground.core-test
  (:require [clojure.core.async :as async])
  (:import [com.launchdarkly.eventsource EventHandler EventSource$Builder]
           (java.net URI)
           (okhttp3 Headers$Builder)
           (clojure.core.async.impl.channels ManyToManyChannel)))

(defn set-cleanup! [chan f]
  (add-watch (.closed ^ManyToManyChannel chan)
    "channel-resource-cleanup"
    (fn [_ _ old-state new-state]
      (when (and (not old-state) new-state)
        (f))))
  chan)

(def default-headers
  {})

(defn handler [chan]
  (reify EventHandler
    (onMessage [_ event message-event]
      (async/put! chan (.getData message-event)))
    (onOpen [_]
      (println "Connection established!"))
    (onError [_ e]
      (println "Encountered error!"))
    (onClosed [_]
      (async/close! chan))))

(defn make-headers [headers]
  (let [builder (Headers$Builder.)]
    (.build (reduce (fn [b [k v]]
                      (.add b (name k) (name v)))
              builder headers))))

(defn listen [url & [{:keys [headers] :or {headers {}}}]]
  (let [output (async/chan)
        source (-> (EventSource$Builder. (handler output) (URI. url))
                 (.headers (make-headers (merge default-headers headers)))
                 (.build))]
    (.start source)
    (set-cleanup! output
      #(.close source))))


(def channel (listen "http://localhost:3000/api/subscribe/customer"))

(async/go-loop [chan channel]
  (let [event (async/<! chan)]
    (println event))
  (recur chan))