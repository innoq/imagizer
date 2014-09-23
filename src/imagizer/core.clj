(ns imagizer.core
  (:require [ring.middleware.reload :as reload]
            [ring.adapter.jetty :as ring]
            [ring.util.response :as response]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.handler :as h]
            [hiccup.core :as hiccup]
            [hiccup.form :as form]
            [clj-http.client :as http]
            [hickory.core :as hickory])
  (:import [org.im4java.core ConvertCmd IMOperation]))

(def workdir "work")

(defn random-filename []
  (str workdir "/" (java.util.UUID/randomUUID)))

(defn write-file [byte-array filename]
   (with-open [stream (java.io.BufferedOutputStream. (java.io.FileOutputStream. filename))]
     (.write stream byte-array)))

(defn paint [from to]
  (let [op  (-> (IMOperation.)
                (.addImage (into-array String [from]))
                (.paint 2.0)
                (.addImage (into-array String [to])))]
    (.run (ConvertCmd.) op (into-array Object []))))

(defn download-to-file [url filename]
  (-> url
      (http/get {:as :byte-array})
      :body
      (write-file filename)))

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
   (form/form-to [:get "/images"]
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
                         [:a {:href (str "/image?src=" src)}
                          [:img {:src src :alt alt}]]])
                      sources-and-alts))))

(defn image-page [src]
  (hiccup/html 
   [:img {:src src}]
   (form/form-to [:post (str "/image?src=" src)]
                 (form/submit-button "convert"))))

(defn convert-image [src]
  (let [[from to] (repeatedly random-filename)]
    (download-to-file src from)
    (paint from to)
    (response/redirect-after-post (str "/result?f=" (last (.split to "/"))))))

(defn result-page [f]
  (hiccup/html [:img {:src (str "/static/" f)}]))

(defroutes app-routes
  (GET "/" [] homepage)
  (GET "/images" [url] (images-page url))
  (GET "/image" [src] (image-page src))
  (POST "/image" [src] (convert-image src))
  (GET "/result" [f] (result-page f))
  (route/files "/static" {:root workdir})
  (route/not-found (hiccup/html [:h1 "page not found"])))

(def webapp (h/api app-routes))

;; only for development
(defn start-dev-server [port handler]
  (let [rh (reload/wrap-reload handler)]
    (ring/run-jetty rh {:port port :join? false})))

;;(defonce server (start-dev-server 9000 #'webapp))

;; (webapp {}) -> 404
;; (webapp {:uri "/" :request-method :get}) -> 202
