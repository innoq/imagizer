-- name: add-image!
INSERT INTO Image(Id, Origin, Filter) VALUES (:uuid, :origin, :filter);
