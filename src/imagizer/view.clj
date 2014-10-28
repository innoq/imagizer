(ns imagizer.view
  (:require [hiccup.page :as hiccup]
            [hiccup.form :as form]
            [ring.util.response :as response]))

(def baseurl "http://localhost:3000")

(defn cache-forever [resp]
  (response/header resp "Cache-Control" "max-age=31536000"))

(defn layout [& content]
  (response/response 
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
     (hiccup/include-js "/js/imagizer.js")])))

(defn search-form [url]
  (form/form-to [:get "/images"]
                [:p (form/text-field {:size 50} "url" url)]
                [:p (form/submit-button "search")]))

(def homepage
  (layout
   [:p "Search for images"]
   (search-form "http://picjumbo.com/category/animals/")))

(defn images-page [url sources-and-alts]
  (layout [:h1 "search result"]
          (search-form url)
          [:ul.result-list
           (map (fn [[src alt]]
                  [:li.result
                   [:a {:href (str "/image?src=" src)}
                    [:img {:src src :alt alt}]]])
                sources-and-alts)]))

(defn image-page [src conversions]
  (layout
   [:div.image-preview
    [:img.original {:src src}]
    [:img.preview]
    [:div.filter-options
     (form/form-to [:post (str "/image?src=" src)]
                   (map-indexed (fn [idx op]
                                  [:div.filter-option
                                   (form/radio-button {:id (name op) 
                                                       :class "imagechanger"
                                                       :data-preview-url (str "/preview?op=" (name op) "&src="src)}
                                                      "op" 
                                                      (= idx 0) 
                                                      (name op))
                                   (form/label (name op) (name op))])
                                conversions)
                   [:div.filter-action 
                    (form/submit-button "convert")])]]))

(defn image-preview [img-uuid]
  (let [img-src (str "/static/" img-uuid)]
    (cache-forever (layout
                    [:div.filter-result
                     [:img.filtered {:src img-src}]]))))

(defn tag-list [tags]
  [:ul.tags
   (map (fn [tag]
          [:li [:a {:href (str "/tags/" tag)} tag]])
        tags)])

(defn tag-form [uuid]
  (form/form-to [:post (str "/result/" uuid "/tags")]
                [:p (form/text-field {:size 50} "tag" "")]
                [:p (form/submit-button "add tag")]))

(defn result-page [image image-tags]
  (let [img-src (str "/static/" (:id image))
        tags (map :tag image-tags)]
    (when-not (nil? image)
      (layout
       [:div.filter-result
        [:img.filtered {:src img-src}]
        [:p "original: " [:a {:href (:origin image)} (:origin image)]]
        [:p "filter: " (:filter image)]
        [:p "share this image: "]
        [:p (form/text-field {:readonly true :id "share" :size 80} "share" (str baseurl img-src))]
        [:p (form/label "tag" "Tags:")]
        (tag-list tags)
        (tag-form (:id image))]))))

(defn image-link [uuid]
  (let [result-link (str "/result/" uuid)
        img-link (str "/static/" uuid)]
    [:a {:href result-link} [:img {:src img-link}]]))

(defn tag-page [images]
  (layout 
   [:ul.result-list
    (map (fn [image] [:li.result (image-link (:id image))])
         images)]))
