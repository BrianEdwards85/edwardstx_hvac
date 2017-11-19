(ns us.edwardstx.service.hvac.transition)

(def threshold 0.5)

(defn delta [f {:keys [last temp] :as st}]
  (-
   (if-let [remote (-> st :remote :temperature)]
           (f remote last)
           last)
   temp))

(defn heat-active? [s]
  (let [d (delta min s)]
    (if (= :off (:status s))
      (> (- threshold) d)
      (> threshold d))))

(defn cool-active? [s]
  (let [d (delta max s)]
    (if (= :off (:status s))
      (< threshold d)
      (< (- threshold) d))))

(defn get-new-status [s]
  (let [m (:mode s)]
    (cond
      (= :off  m) :off
      (= :fan  m) :fan
      (and (= :cool m) (cool-active? s)) m
      (and (contains? #{:eheat :heat} m) (heat-active? s)) m
      :else :off)))

