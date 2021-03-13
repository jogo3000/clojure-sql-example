-- :name drop-all-tables :!
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;

-- :name create-heimo-table :!
CREATE TABLE heimo (
  id           SERIAL,
  tieteellinen TEXT,
  nimi         TEXT,

  PRIMARY KEY (id)
);

-- :name create-laji-table :!
CREATE TABLE laji (
  id           SERIAL,
  tieteellinen TEXT,
  nimi         TEXT,
  "heimo-id"   INTEGER,

  PRIMARY KEY (id),
  FOREIGN KEY ("heimo-id") REFERENCES heimo(id) ON DELETE CASCADE
);

-- :name insert-heimo :<!
INSERT INTO heimo (tieteellinen, nimi) VALUES (:tieteellinen, :nimi) returning *;

-- :name insert-laji :<!
INSERT INTO laji ("heimo-id", tieteellinen, nimi) VALUES (:heimo-id, :tieteellinen, :nimi) returning *;

-- :name select-all-laji :? :*
SELECT * FROM laji;

-- :name select-all-heimo :? :*
SELECT * from heimo;

-- :name select-hierarchy :? :*
SELECT
  l.tieteellinen as "tieteellinen",
  l.nimi as "nimi",
  h.tieteellinen as "heimo-tieteellinen",
  h.nimi as "heimo-nimi"
FROM laji l
JOIN heimo h ON l."heimo-id" = h.id
