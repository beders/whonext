(ns whonext.app
  (:require [re-frame.core :as rf]
            [clojure.string :as str]))

;;; views

(defn current-person []
  [:center [:h2 (or @(rf/subscribe [::sub.current-person]) "Done!")]])

(defn action-bar []
  [:div
   (if @(rf/subscribe [::sub.has-next?]) [:button {:on-click #(rf/dispatch [::evt.next!])} "Next!"]
                                         [:button.secondary {:on-click #(rf/dispatch [::evt.load-persons])} "Restart!"])])

(defn previous-persons []
  [:ol
   (for [person @(rf/subscribe [::sub.previous-persons])]
     [:li person]
     )])

(defn setup []
  (let [url @(rf/subscribe [::sub.setup-names-url])]
    [:article
     [:hgroup [:h3 "Never fight over who goes next again!"]
      [:h4 "Save your introverts from picking who goes next at standup"]]
     [:div "Add names separated by comma:"]
     [:input.names {:placeholder "Huey,Dewey,Louie" :on-change #(rf/dispatch [::setup-names (-> % .-target .-value)])}]
     [:div "Then bookmark this link 👇"]
     [:div
      [:strong [:a {:href url :style {:overflow-wrap :anywhere}} url]]]
     [:div "We'll present the names in a different order on re-load thanks to our patent-pending (shuffle names) technology!"]
     [:br]
     (when (seq @(rf/subscribe [::sub.setup-names]))
       [:<>
        [:div "To have this appear inside a JIRA (cloud) board, bookmark this link 👉 " [:a {:href @(rf/subscribe [::sub.bookmarklet])} "Whonext-Jira"]]
        [:div "Then click the bookmark when on a JIRA scrum board page."]])
     ]))

(defn normal-version []
  [:div
   [current-person]
   [action-bar]
   [previous-persons]])

(defn mini-version []
  (if @(rf/subscribe [::sub.has-next?])
    [:button.mini-button {:on-click #(rf/dispatch [::evt.next!])} @(rf/subscribe [::sub.current-person])]
    [:button.mini-button.secondary {:on-click #(rf/dispatch [::evt.load-persons])} "Restart!"]))

(defn main-panel []
  (let [mini? (= :mini @(rf/subscribe [::sub.version]))]
    [:div
     [:main.container {:style {:margin-top "4px"}}
      (when-not mini? [:nav [:ul [:li [:strong "Who next?"]]] [:ul [:li [:a {:href "/"} [:small "Start fresh!"]]]]])
      (if @(rf/subscribe [::sub.setup?])
        [setup]
        (if mini?
          [mini-version]
          [normal-version]))]
     (when @(rf/subscribe [::sub.setup?])
       [:footer
        [:div [:small "Made with 🧡  by " [:a {:href "https://twitter.com/beders"
                                              } "beders"]]]
        [:div [:small "Thanks to " [:a {:href "https://day8.github.io/re-frame/"} "re-frame"] " and " [:a {:href "https://picocss.com"} "Pico CSS"]]]
        [:div [:small "Honestly, this could have been 4 lines of JS code but instead turned into a re-frame tutorial"]]])
     ]))

;;; subs
(rf/reg-sub
 ::sub.persons
 (fn [db]
   (:persons db)))

(rf/reg-sub
 ::sub.current-person
 :<- [::sub.persons]
 (fn [persons]
   (first persons)))

;;; sugar for
(rf/reg-sub
 ::sub.current-person'
 (fn [_query-vector]
   (rf/subscribe [::sub.persons]))
 (fn [persons _query-vector]
   (first persons)))

(rf/reg-sub
 ::sub.has-next?
 :<- [::sub.current-person]
 (fn [current]
   (some? current)))

(rf/reg-sub
 ::sub.previous-persons
 #(:previous %))

(rf/reg-sub
 ::sub.version
 #(:version %))

(rf/reg-sub
 ::sub.setup?
 #(:setup %))

(rf/reg-sub
 ::sub.setup-names
 #(:setup-names %))

(rf/reg-sub
 ::sub.setup-names-url
 :<- [::sub.setup-names]
 (fn [names]
   (str (-> js/location .-origin) "/?p=" (some-> names (js/encodeURIComponent names)))))

(rf/reg-sub
 ::sub.bookmarklet
 :<- [::sub.setup-names-url]
 (fn [url]
   (let [bookmarklet "javascript:(function(){e = document.querySelector('#ak-jira-navigation nav [data-testid=\"create-button-wrapper\"]').parentElement;
      f = document.createElement('iframe');
      e.appendChild(f);
      f.src='
      "
         end         "';})();"
         mini-version "&v=mini"
         ]
     (str bookmarklet url mini-version end))
   ))

;;; events

(rf/reg-fx
 ::fx.from-url
 (fn []
   (let [params (-> js/window .-location .-search js/URLSearchParams.)
         persons (->> (-> params (.get "p") (str/split #","))
                      (map str/trim)
                      (filter (complement empty?))
                      (into []))
         version (keyword (or (.get params "v") "normal"))]
     (if (empty? persons)
       (rf/dispatch [::evt.setup])
       (rf/dispatch [::evt.initialize-db persons version])
       ))))

(rf/reg-event-fx
 ::evt.load-persons
 (fn []
   {::fx.from-url {}}))

(rf/reg-event-db
 ::evt.setup
 (fn [db]
   (assoc db :setup true)))

(rf/reg-event-db
 ::evt.initialize-db
 (fn [_ [_ persons version]]
   {:setup    false
    :version version
    :persons  (shuffle persons)
    :previous []}))

(rf/reg-event-db
 ::evt.next!
 (fn [db _]
   (let [[current & rest] (:persons db)]
     (-> db
         (assoc :persons rest)
         (update :previous conj current)))))

(rf/reg-event-db
 ::setup-names
 (fn [db [_ names]]
   (assoc db :setup-names names)))

;
(rf/reg-event-fx
 :bug-empty
 (fn []
   {:db {}}))

(comment
 @re-frame.db/app-db
 @(rf/subscribe [::sub.persons])
 @(rf/subscribe [::sub.setup-names])
 @(rf/subscribe [::sub.version])
 (rf/dispatch [::evt.initialize-db ["Tick" "Trick" "Track"] :normal])
 (rf/dispatch [::evt.next!])
 (tap> @re-frame.db/app-db)
 )