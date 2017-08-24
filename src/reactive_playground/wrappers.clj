(ns reactive-playground.wrappers
  (:require [ring.middleware.json :as json]
            [ring.middleware.defaults :as defaults]
            [selmer.middleware :refer [wrap-error-page]]
            [ninjudd.eventual.server :refer [json-events]]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]
            [taoensso.timbre :as logger]
            [ring.middleware.reload :as rreload]
            [selmer.parser :as selmer]
            [clojure.core.async :as async]
            [ring.util.response :as response]))

(defn shim-synchronous-handlers [handler]
  (fn
    ([request] (handler request))
    ([req resp raise]
     (try
       (resp (handler req))
       (catch Exception e
         (raise e))))))

(defn wrap-error-logging [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (logger/error e "")))))

(defn wrap-local [handler]
  (-> handler
    (rreload/wrap-reload)
    (wrap-error-logging)))

(defn wrap-json [handler]
  (-> handler
    (json/wrap-json-body {:keywords? true})
    (json/wrap-json-params)
    (json/wrap-json-response)))

(defn wrap-default-middleware [handler]
  (defaults/wrap-defaults handler defaults/site-defaults))

(defn wrap-server-sent [handler]
  (fn [request]
    (let [{:keys [body] :as response} (handler request)]
      (if (= (class (async/chan)) (class body))
        (json-events body)
        response))))

(defn global-selmer-context []
  {:csrf *anti-forgery-token*})

(defn wrap-selmer [handler]
  (->
    (fn [request]
      (let [{[template context] :body :as response} (handler request)]
        (if (and template context)
          (->
            response
            (assoc :body (selmer/render-file template (merge context (global-selmer-context))))
            (response/content-type "text/html"))
          response)))
    (wrap-error-page)))
