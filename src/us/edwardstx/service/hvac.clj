(ns us.edwardstx.service.hvac
  (:require [com.stuartsierra.component   :as component]
            [config.core                  :refer [env]]
            [clojure.tools.logging        :as log]
            [manifold.deferred            :as d]
            [us.edwardstx.service.hvac.display :refer [new-display]]
            [us.edwardstx.service.hvac.temp :refer [new-temp]]
            [us.edwardstx.service.hvac.state :refer [new-state]]
            [us.edwardstx.service.hvac.orchestrator :refer [new-orchestrator]]
            [us.edwardstx.service.hvac.handler :refer [new-handler]]
            [us.edwardstx.common.conf     :refer [new-conf]]
            [us.edwardstx.common.events   :refer [new-events]]
            [us.edwardstx.common.keys     :refer [new-keys]]
            [us.edwardstx.common.logging  :refer [new-logging]]
            [us.edwardstx.common.rabbitmq :refer [new-rabbitmq]]
            [us.edwardstx.common.tasks    :refer [new-tasks]]
            [us.edwardstx.common.token    :refer [new-token]])
  (:import [com.pi4j.wiringpi Gpio])
  (:gen-class))

(defonce system (atom {}))

(defn init-system [env]
  (component/system-map
   :keys (new-keys env)
   :token (new-token env)
   :conf (new-conf env)
   :logging (new-logging)
   :tasks (new-tasks)
   :rabbitmq (new-rabbitmq)
   :events (new-events)
   :display (new-display)
   :temp (new-temp 5000)
   :state (new-state)
   :orchestrator (new-orchestrator)
   :handler (new-handler)
   ))


(defn -main [& args]
  (let [semaphore (d/deferred)]
    (Gpio/wiringPiSetupSys)
    (reset! system (init-system env))

    (swap! system component/start)

    (println "Started")

    (log/info "HVAC Service started")

    (deref semaphore)

    (component/stop @system)

    (shutdown-agents)
    ))

