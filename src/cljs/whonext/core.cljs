(ns whonext.core
  (:require
   [reagent.dom :as rdom]
   [re-frame.core :as re-frame]
   [whonext.app :as app]
   ))


(defn dev-setup []
  (when goog.DEBUG
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [app/main-panel] root-el)))

(defn init []
  (re-frame/dispatch-sync [::app/evt.load-persons])
  (dev-setup)
  (mount-root))
