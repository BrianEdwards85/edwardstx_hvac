(ns us.edwardstx.hvac.transition)

(def threshold 0.5)

(defn set-mode! [m s]
  (swap! s #(assoc % :mode m)))

(defn set-temp! [t s]
  (swap! s #(assoc % :temp t)))

(defn delta [s]
  (- (:last s) (:temp s)))

(defn heat-active? [s]
  (let [d (delta s)]
    (if (= :off (:status s))
      (> (- threshold) d)
      (> threshold d))))

(defn cool-active? [s]
  (let [d (delta s)]
    (if (= :off (:status s))
      (< threshold d)
      (< (- threshold) d))))

(defn get-new-state [s]
  (let [m (:mode s)]
    (cond
      (= :off  m) :off
      (= :fan  m) :fan
      (and (= :cool m) (cool-active? s)) m
      (and (contains? #{:eheat :heat} m) (heat-active? s)) m
      :else :off)))

