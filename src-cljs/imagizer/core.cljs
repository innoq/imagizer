(ns imagizer.core
  (:require [dommy.utils :as utils]
            [dommy.core :as dommy]
            [cljs-http.client :as http]
            [cljs.core.async :refer [chan put! <!]]
            [hickory.core :as hickory])
  (:use-macros [dommy.macros :only [node sel sel1]]
               [cljs.core.async.macros :only [go go-loop]]))

(defn load-image [url]
    (http/get url))

(defn changes [changers]
  (let [channel (chan)]
    (doseq [c changers]
      (let [publish-preview (fn []
                              (when (.-checked c)
                                (put! channel (dommy/attr c :data-preview-url))))]
        (publish-preview)
        (dommy/listen! c :click publish-preview)))
    channel))

(defn enhance-imagepreview! [elem]
  (let [preview (sel1 elem :.preview)
        preview-urls (changes (sel elem :.imagechanger))]
    (go-loop []
             (let [preview-url (<! preview-urls)
                   resp (<! (load-image preview-url))
                   new-src (-> resp :body hickory/parse (sel1 :img.filtered) .-src)]
               (dommy/set-attr! preview :src new-src)
               (recur)))))

(doseq [img-prev (sel :.image-preview)]
  (enhance-imagepreview! img-prev))

