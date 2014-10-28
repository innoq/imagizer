(ns imagizer.images.processing
  (:require [yesql.core :refer [defquery defqueries]])
  (:import [org.im4java.core ConvertCmd IMOperation]))

(defquery get-image "db/get_image.sql")
(defquery get-images-by-tag "db/get_images_by_tag.sql")

(defqueries "db/add_image.sql")

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


