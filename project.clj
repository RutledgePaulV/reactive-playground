(defproject reactive-playground "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0-alpha17"]
                 [ring "1.6.2"]
                 [ring/ring-defaults "0.3.1"]
                 [ring/ring-json "0.5.0-beta1"]
                 [com.ninjudd/eventual "0.5.5"]
                 [cheshire "5.7.1"]
                 [mount "0.1.11"]
                 [selmer "1.10.9"]
                 [compojure "1.6.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [com.coreos/jetcd-core "0.0.1"]
                 [org.clojure/core.async "0.3.443"]
                 [com.launchdarkly/okhttp-eventsource "1.5.2"]]

  :main reactive-playground.core)
