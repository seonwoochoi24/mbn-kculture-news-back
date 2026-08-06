ALTER TABLE article
    ADD COLUMN image_url VARCHAR(2048) NULL AFTER description,
    ADD COLUMN journalist_name VARCHAR(200) NULL AFTER image_url;
