(ns imagizer.core
  (:require [ring.middleware.reload :as reload]
            [ring.adapter.jetty :as ring]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.handler :as h]
            [hiccup.core :as hiccup]
            [hiccup.form :as form]
            [clj-http.client :as http]
            [hickory.core :as hickory]))

(defn load-html [url]
  (-> url http/get :body))

(defn parse-to-hiccup [str]
  (-> str hickory/parse hickory/as-hiccup))

(defn children [hiccup-html]
  (cond
   (not (vector? hiccup-html)) hiccup-html
   (map? (second hiccup-html)) (drop 2 hiccup-html)
   :else (rest hiccup-html)))

(defn all-tags [hiccup-html]
  (tree-seq #(or (seq? %) (vector? %)) children hiccup-html))

(defn img? [elem]
  (= :img (first elem)))

(defn attribute [attr elem]
  (when (map? (second elem))
    (get (second elem) attr)))

(def src (partial attribute :src))

(def alt (partial attribute :alt))

(defn imgs [hiccup-html]
  (let [tags (all-tags hiccup-html)]
    (filter img? tags)))

(def homepage
  (hiccup/html
   (form/form-to [:get "images"]
                 (form/text-field {:size 50} "url" "http://jax.de/wjax2014/speakers")
                 (form/submit-button "show"))))

(defn images-page [url]
  (let [images (-> url load-html parse-to-hiccup imgs)
        sources-and-alts (->> images
                              (filter #(.startsWith (src %) "http"))
                              (map (juxt src alt)))]
    (hiccup/html [:h1 url]
                 (map (fn [[src alt]]
                        [:div
                         [:p src]
                         [:p alt]
                         [:img {:src src :alt alt}]])
                      sources-and-alts))))

(defroutes app-routes
  (GET "/" [] homepage)
  (GET "/images" [url] (images-page url))
  (route/not-found (hiccup/html [:h1 "page not found"])))

(def webapp (h/api app-routes))

;; only for development
(defn start-dev-server [port handler]
  (let [rh (reload/wrap-reload handler)]
    (ring/run-jetty rh {:port port :join? false})))

;;(defonce server (start-dev-server 9000 #'webapp))
