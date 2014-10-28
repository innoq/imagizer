(ns imagizer.core
  (:require [ring.util.response :as response]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.middleware.file-info :refer [wrap-file-info]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [imagizer.images.retrieval :as ir]
            [imagizer.images.processing :as ip]
            [imagizer.tags :as t]
            [imagizer.view :as v]))

(def db-spec {:classname "org.h2.Driver"
              :subprotocol "h2:file"
              :subname "./db/data"})

(def filtered-file-type ".jpg")

(def workdir "work")

(defn random-filename []
  (str workdir "/" (java.util.UUID/randomUUID)))

(defn convert-image [src op]
  (let [[from to-path] (repeatedly random-filename)
        to-file (str to-path filtered-file-type)
        to-id (last (.split to-path "/"))]
    (ir/download-to-file src from)
    ((-> op ip/conversion ip/converter) from to-file)
    to-id))

(defn store-image-info [uuid src op]
  (ip/add-image! db-spec uuid src op)
  (t/add-tag! db-spec uuid op))

(defn convert-and-store-image [src op]
  (let [uuid (convert-image src op)]
    (store-image-info uuid src op)
    (response/redirect-after-post (str "/result/" uuid))))

(defn add-tag [uuid tag]
  (if-not (nil? tag)
    (t/add-tag! db-spec uuid tag))
  (response/redirect-after-post (str "/result/" uuid)))

(defn tags-json []
  (let [tags (t/all-img-tags db-spec)]
    (response/response (map :tag tags))))

(defn filtered-file-by-uuid [uuid]
  (str workdir "/" uuid filtered-file-type))

(defn filtered-file [uuid]
  (v/cache-forever (response/file-response (filtered-file-by-uuid uuid))))

(defn images-page [url]
  (let [images (-> url ir/load-html ir/parse-to-hiccup ir/imgs)
        sources-and-alts (->> images
                              (filter #(.startsWith (ir/src %) "http"))
                              (remove #(.startsWith (ir/src %) "https"))
                              (map (juxt ir/src ir/alt)))]
    (v/images-page url sources-and-alts)))

(defn result-page [uuid]
  (let [image (first (ip/get-image db-spec uuid))
        image-tags (t/get-image-tags db-spec uuid)]
    (v/result-page image image-tags)))

(defroutes app-routes
  (GET "/" [] v/homepage)
  (GET "/images" [url] (images-page url))
  (GET "/image" [src] (v/image-page src (keys ip/conversions)))
  (POST "/image" [src op] (convert-and-store-image src op))
  (GET "/preview" [src op] (v/image-preview (convert-image src op)))
  (GET "/result/:uuid" [uuid] (result-page uuid))
  (POST "/result/:uuid/tags" [uuid tag] (add-tag uuid tag))
  (GET "/tags" [] (tags-json))
  (GET "/tags/:tag" [tag] (v/tag-page (ip/get-images-by-tag db-spec tag)))
  (GET "/static/:uuid" [uuid] (filtered-file uuid))
  (route/resources "/")
  (route/not-found "oops - not found"))

(defn wrap-default-content-type [handler content-type]
  (fn [req]
    (let [resp (handler req)]
      (if (contains? (:headers resp) "Content-Type")
        resp
        (assoc-in resp [:headers "Content-Type"] content-type)))))

(def webapp (-> app-routes
                wrap-json-response
                (wrap-defaults (assoc-in api-defaults [:responses :content-types] false))
                wrap-file-info
                (wrap-default-content-type "text/html")))
