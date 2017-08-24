(ns reactive-playground.core
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.adapter.jetty :as jetty]
            [reactive-playground.handlers :as handlers]
            [reactive-playground.wrappers :as wrappers]))


(defroutes public-routes
  (->
    (routes
      (GET "/healthz" []
        {:body {:health true}}))
    (wrappers/wrap-json)))


(defroutes api-routes

  (POST "/api/:resource/:id" [resource id :as request]
    {:body (handlers/transact! request resource id (:json-params request {}))})

  (->
    (routes
      (GET "/api/subscribe/:resource" [resource :as request]
        {:body (handlers/subscribe request resource)}))
    (wrappers/wrap-server-sent)))


(defroutes web-routes

  (route/resources "/static" {:root "./static"})

  (->
    (routes
      (GET "/" [] {:body ["base.vue" {}]}))
    (wrappers/wrap-selmer)))


(defroutes application
  (->

    (routes
      public-routes
      web-routes
      api-routes
      (route/not-found "Not found!"))
    (wrappers/wrap-json)
    (wrappers/wrap-local)
    (wrappers/shim-synchronous-handlers)
    (wrappers/wrap-default-middleware)))


(defn -main [& args]
  (require '[reactive-playground.bootstrap])
  (jetty/run-jetty application
    {:async? true :port 3000}))