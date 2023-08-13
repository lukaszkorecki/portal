(ns ^:no-doc portal.ui.connection-status
  (:require ["react" :as react]
            [portal.ui.state :as state]
            [reagent.core :as r]))

(defn- use-interval [f milliseconds]
  (react/useEffect
   (fn []
     (when (fn? f)
       (let [interval (js/setInterval f milliseconds)]
         (fn [] (js/clearInterval interval)))))
   #js [f]))

(defn- timeout [ms]
  (js/Promise.
   (fn [_resolve reject]
     (js/setTimeout
      #(reject (ex-info "Timeout reached" {:duration ms})) ms))))

(def ^:private poll-interval-ms 5000)

(defn- use-conn-poll []
  (let [state (state/use-state)]
    (use-interval
     (fn []
       (-> (.race js/Promise
                  [(state/invoke 'portal.runtime/ping)
                   (timeout poll-interval-ms)])
           (.then  (fn [_]
                     ;; reconnecting to runtime
                     (let [connected (::connected @state)]
                       (when-not connected
                         (when (false? connected) (state/reset-value-cache!))
                         (state/dispatch! state assoc ::connected true)))))
           (.catch (fn [_]
                     (when (::connected @state)
                       (state/dispatch! state assoc ::connected false))))))
     poll-interval-ms)))

(defn poller [] (use-conn-poll) nil)

(defn use-status [] (not (false? @(r/cursor (state/use-state) [::connected]))))