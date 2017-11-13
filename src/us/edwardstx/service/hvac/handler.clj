(ns us.edwardstx.service.hvac.handler
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging      :as log]
            [us.edwardstx.common.tasks :as tasks]
            [us.edwardstx.service.hvac.state :as state]
            [us.edwardstx.service.hvac.controller :refer [status-map]]
            [manifold.stream :as s]
            [manifold.deferred :as d]))

(def status-set (set (keys status-map)))

(defn parseInteger [x]
  (try
    (Integer/parseInt x)
    (catch Exception e nil)))

(defn mode-handler [state {:keys [body response]}]
  (d/success! response true)
  (let [mode (keyword body)]
    (if (status-set mode)
      (state/update-state state :mode mode))))

(defn temp-handler [state {:keys [body response]}]
  (d/success! response true)
  (if-let [temp (parseInteger body)]
    (state/update-state state :temp temp)))

(defn map-handler [state {:keys [body response]}]
  (d/success! response true)
  (if (map? body)
    (let [{:keys [mode temp]} body]
      (if (and mode (status-set mode))
        (state/update-state state :mode mode))
      (if (and temp (integer? temp))
        (state/update-state state :temp temp))

      )))

(defn create-handlers [state tasks]
  (let [mode-handler-stream (tasks/task-subscription tasks "state.mode.update")
        temp-handler-stream (tasks/task-subscription tasks "state.temp.update")
        map-handler-stream (tasks/task-subscription tasks "state.update")]
    (s/consume (partial map-handler state) map-handler-stream)
    (s/consume (partial mode-handler state) mode-handler-stream)
    (s/consume (partial temp-handler state) temp-handler-stream)
    (list mode-handler-stream temp-handler-stream)
    )
  )

(defrecord Handler [state tasks streams]
  component/Lifecycle

  (start [this]
    (assoc this :streams (create-handlers state tasks)))

  (stop [this]
    (->> this
         :streams
         (map s/close!)
         doall)
    (assoc this :streams nil)))

(defn new-handler []
  (component/using
   (map->Handler {})
   [:state :tasks]))
