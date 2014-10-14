-- name: add-tag!
INSERT INTO Image_Tag(Tag, Image) VALUES (:tag, :file);
