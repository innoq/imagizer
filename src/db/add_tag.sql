-- name: add-tag!
INSERT INTO Image_Tag(Image, Tag) VALUES (:file, :tag);
