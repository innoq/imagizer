SELECT Id, Origin, Filter FROM Image_Tag
LEFT JOIN Image ON Image = Id
WHERE Tag = :tag;
