(ns whonext.app
  (:require [re-frame.core :as re]
            [clojure.string :as str]))

;; some syntactic sugar: (<== ... ) vs @(re/subscribe ...)
(def <== (comp deref re/subscribe))

;; views

(defn current-person []
  [:center [:h2 (or (<== [::sub.current-person]) "Done!")]])

(defn action-bar []
  [:div
   (if (<== [::sub.has-next?]) [:button {:on-click #(re/dispatch [::evt.next!])} "Next!"]
                               [:button.secondary {:on-click #(re/dispatch [::evt.load-persons])} "Restart!"])])

(defn previous-persons []
  [:ol
   (for [person (<== [::sub.previous-persons])]
     [:li person]
     )])

(defn setup []
  (let [url (<== [::sub.setup-names-url])]
    [:article
     [:hgroup [:h3 "Never fight over who goes next again!"]
      [:h4 "Save your introverts from picking who goes next at standup"]]
     [:div "Add names separated by comma:"]
     [:input.names {:placeholder "Huey,Dewey,Louie" :on-change #(re/dispatch [::setup-names (-> % .-target .-value)])}]
     [:div "Then click and bookmark this link 👇"]
     [:div
      [:strong [:a {:href url :style {:overflow-wrap :anywhere}} url]]]
     [:div "We'll present the names in a different order on re-load thanks to our patent-pending (shuffle names) technology!"]]))

(defn main-panel []
  [:div
   [:main.container
    [:nav  [:ul [:li [:strong "Who next?"]]] [:ul [:li [:a {:href "/"} [:small "Start fresh!"]]]]]
    (if (<== [::sub.setup?])
      [setup]
      [:div
       [current-person]
       [action-bar]
       [previous-persons]])]
   (when (<== [::sub.setup?])
     [:footer
      [:div [:small "Made with 🧡  by " [:a {:href "https://twitter.com/beders"
                                             } "beders"]]]
      [:div [:small "Thanks to " [:a {:href "https://day8.github.io/re-frame/"} "re-frame"] " and " [:a {:href "https://picocss.com"} "Pico CSS"]]]
      [:div [:small "Honestly, this could have been 4 lines of JS code but instead turned into a re-frame tutorial"]]])
   ])

;; subs
(re/reg-sub
  ::sub.persons
  #(:persons %))

(re/reg-sub
  ::sub.current-person
  :<- [::sub.persons]
  (fn [persons]
    (first persons)))

(re/reg-sub
  ::sub.has-next?
  :<- [::sub.current-person]
  (fn [current]
    (some? current)))

(re/reg-sub
  ::sub.previous-persons
  #(:previous %))

(re/reg-sub
  ::sub.setup?
  #(:setup %))

(re/reg-sub
  ::sub.setup-names
  #(:setup-names %))

(re/reg-sub
  ::sub.setup-names-url
  :<- [::sub.setup-names]
  (fn [names]
    (str (-> js/location .-origin) "/?p=" (some-> names (js/encodeURIComponent names)))))

;; events

(re/reg-fx
  ::fx.from-url
  (fn []
    (let [persons
          (->> (-> js/window .-location .-search js/URLSearchParams. (.get "p") (str/split #","))
               (map str/trim)
               (filter (complement empty?))
               (into [])
               )]
      (if (empty? persons)
        (re/dispatch [::evt.setup])
        (re/dispatch [::evt.initialize-db persons])
        ))))

(re/reg-event-fx
  ::evt.load-persons
  (fn []
    {::fx.from-url {}}))

(re/reg-event-db
  ::evt.setup
  (fn [db]
    (assoc db :setup true)))

(re/reg-event-db
  ::evt.initialize-db
  (fn [_ [_ persons]]
    {:setup false
     :persons  (shuffle persons)
     :previous []}))

(re/reg-event-db
  ::evt.next!
  (fn [db _]
    (let [[current & rest] (:persons db)]
      (-> db
          (assoc :persons rest)
          (update :previous conj current)
          )
      )))

(re/reg-event-db
  ::setup-names
  (fn [db [_ names]]
    (assoc db :setup-names names)))


