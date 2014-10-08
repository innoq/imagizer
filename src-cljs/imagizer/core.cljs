(ns imagizer.core
  (:require [dommy.utils :as utils]
            [dommy.core :as dommy]
            [cljs-http.client :as http]
            [cljs.core.async :refer [chan put! <!]]
            [hickory.core :as hickory])
  (:use-macros [dommy.macros :only [node sel sel1]]
               [cljs.core.async.macros :only [go go-loop]]))

(defn load-image [url op]
    (http/post url {:form-params {:op op}}))

(defn changes [changers]
  (let [channel (chan)]
    (doseq [c changers]
      (let [publish-op (fn []
                         (when (.-checked c)
                           (put! channel (.-value c))))]
        (publish-op)
        (dommy/listen! c :click publish-op)))
    channel))

(defn enhance-imagepreview! [elem]
  (let [preview (sel1 elem :.preview)
        form (sel1 elem :form)
        loader (partial load-image (dommy/attr form :action))
        ops (changes (sel elem :.imagechanger))]
    (go-loop []
             (let [op (<! ops)
                   resp (<! (loader op))
                   new-src (-> resp :body hickory/parse (sel1 :img) .-src)]
               (dommy/set-attr! preview :src new-src)
               (recur)))))

(doseq [img-prev (sel :.imagepreview)]
  (enhance-imagepreview! img-prev))

