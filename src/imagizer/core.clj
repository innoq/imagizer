(ns imagizer.core
  (:require [ring.middleware.reload :as reload]
            [ring.adapter.jetty :as ring]
            [ring.util.response :as response]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.handler :as h]
            [hiccup.page :as hiccup]
            [hiccup.form :as form]
            [clj-http.client :as http]
            [hickory.core :as hickory]
            [yesql.core :refer [defquery defqueries]]
            [ring.middleware.json :refer [wrap-json-response]])
  (:import [org.im4java.core ConvertCmd IMOperation]))

(def baseurl "http://localhost:3000")

(def workdir "work")

(defquery all-img-tags "db/all_image_tags.sql")

(defqueries "db/add_tag.sql")

(def db-spec {:classname "org.h2.Driver"
              :subprotocol "h2:file"
              :subname "./db/data"})


(defn random-filename []
  (str workdir "/" (java.util.UUID/randomUUID)))

(defn write-file [byte-array filename]
   (with-open [stream (java.io.BufferedOutputStream. (java.io.FileOutputStream. filename))]
     (.write stream byte-array)))

(def conversions (sorted-map 
                  :paint #(.paint % 2.0)
                  :blur #(.blur % 10.0)
                  :rotate-left #(.transpose %)
                  :rotate-right #(.transverse %)
                  :swirl #(.swirl % 80.0)
                  :spread #(.spread % (int 5))
                  :polaroid #(.polaroid % 5.0)
                  :negate #(.negate %)
                  :monochrome #(.monochrome %)
                  :posterize #(.posterize % (int 5))
                  :mirror-vertically #(.flop %)
                  :mirror-horizontally #(.flip %)
                  :charcoal #(.charcoal % (int 1))
                  ;; http://stackoverflow.com/questions/4058224/creation-of-edge-detection-based-image-in-php#comment4359286_4058233
                  :black-white-sketch #(-> % (.edge 1.0) .negate .normalize (.colorspace "Gray") (.blur 0.5) (.contrastStretch (int 50)))))

(defn conversion [op]
  ((keyword op) conversions))

(defn converter [conversion]
  (fn [from to]
    (let [op (-> (IMOperation.)
                 (.addImage (into-array String [from]))
                 conversion
                 (.addImage (into-array String [to])))]
      (.run (ConvertCmd.) op (into-array Object [])))))

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

(defn layout [& content]
  (hiccup/html5
   [:head
    (hiccup/include-css 
      "http://fonts.googleapis.com/css?family=Montserrat:700,400" 
      "/stylesheets/imagizer.css")
    [:base {:href baseurl}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:meta {:charset "utf-8"}]
    [:title "imagizer"]]
   [:body 
    [:div.nav 
     [:div.nav-content
      [:a {:href "/"}
       [:img {:src "/img/icon.svg"}]
       [:span "Imagizer"]]]
    ]
    [:div.content 
     content]
    (hiccup/include-js "/js/imagizer.js")]))

(defn search-form [url]
  (form/form-to [:get "/images"]
                [:p (form/text-field {:size 50} "url" url)]
                [:p (form/submit-button "search")]))

(def homepage
  (layout
    [:p "Search for images"]
    (search-form "http://jax.de/wjax2014/speakers")))

(defn images-page [url]
  (let [images (-> url load-html parse-to-hiccup imgs)
        sources-and-alts (->> images
                              (filter #(.startsWith (src %) "http"))
                              (map (juxt src alt)))]
    (layout [:h1 "search result"]
            (search-form url)
            [:div.result-list
             (map (fn [[src alt]]
                    [:div.result
                     [:a {:href (str "/image?src=" src)}
                      [:img {:src src :alt alt}]]])
                  sources-and-alts)])))

(defn image-page [src]
  (layout
   [:div.image-preview
    [:img.original {:src src}]
    [:img.preview]
    [:div.filter-options
    (form/form-to [:post (str "/image?src=" src)]
                  (map-indexed (fn [idx op]
                                 [:div.filter-option
                                  (form/radio-button {:id (name op) :class "imagechanger"} "op" (= idx 0) (name op))
                                  (form/label (name op) (name op))])
                               (keys conversions))
                  [:div.filter-action 
                   (form/submit-button "convert")])]]))

(defn convert-image [src op]
  (let [[from toId] (repeatedly random-filename)
        to (str toId ".jpg")]
    (download-to-file src from)
    ((-> op conversion converter) from to)
    (add-tag! db-spec op to)
    (response/redirect-after-post (str "/result?f=" (last (.split to "/"))))))

(defn result-page [f]
  (let [img-src (str "/static/" f)]
    (layout
      [:div.filter-result
       [:img.filtered {:src img-src}]
       [:p "share this image: "]
       (form/text-field {:readonly true :id "share" :size 80} "share" (str baseurl img-src))])))


(defn tags-json []
  (let [tags (all-img-tags db-spec)]
    (response/response (map :tag tags))))

(defroutes app-routes
  (GET "/" [] homepage)
  (GET "/images" [url] (images-page url))
  (GET "/image" [src] (image-page src))
  (POST "/image" [src op] (convert-image src op))
  (GET "/result" [f] (result-page f))
  (GET "/tags" [] (tags-json))
  (route/files "/static" {:root workdir})
  (route/resources "/")
  (route/not-found "oops - not found"))

(def webapp (h/api (wrap-json-response app-routes)))

;; only for development
(defn start-dev-server [port handler]
  (let [rh (reload/wrap-reload handler)]
    (ring/run-jetty rh {:port port :join? false})))

;;(defonce server (start-dev-server 9000 #'webapp))

;; (webapp {}) -> 404
;; (webapp {:uri "/" :request-method :get}) -> 202
