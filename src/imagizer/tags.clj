(ns imagizer.tags
  (:require [yesql.core :refer [defquery defqueries]]))

(defquery all-img-tags "db/all_image_tags.sql")
(defquery get-image-tags "db/get_image_tags.sql")

(defqueries "db/add_tag.sql")
