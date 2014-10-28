(ns imagizer.images.retrieval
  (:require [clj-http.client :as http]
            [hickory.core :as hickory]))

(defn write-file [byte-array filename]
   (with-open [stream (java.io.BufferedOutputStream. (java.io.FileOutputStream. filename))]
     (.write stream byte-array)))

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
