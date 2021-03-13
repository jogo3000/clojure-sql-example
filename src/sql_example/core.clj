(ns sql-example.core
  (:require [clojure.set]
            [clojure.java.jdbc :as jdbc]
            [hugsql.core :as hugsql]))

;;; Lataa SQL-funktiot
(hugsql/def-db-fns "example.sql" {:quoting :ansi})

;;; Tietokannan yhteysparametrit
(def db {:dbtype "postgresql"
         :dbname "postgres"
         :host "localhost"
         :port 5432
         :user "postgres"
         :password "mysecretpassword"})

;;; Alustaa tietokannan
(doto db
  (drop-all-tables)
  (create-heimo-table)
  (create-laji-table))

;;; Syöttää esimerkkidataa tietokantaan
(insert-heimo db {:tieteellinen "Cinclus" :nimi "Koskikarat"})
;; => ({:id 1, :tieteellinen "Cinclus", :nimi "Koskikarat"})
;;; Äskeisen funktion paluuarvosta näkee, että heimon id on 1
(insert-laji db {:heimo-id 1 :tieteellinen "Cinclus cinclus" :nimi "Koskikara"})

;;; Palauttaa kaikki heimot
(select-all-heimo db)
;; => ({:id 1, :tieteellinen "Cinclus", :nimi "Koskikarat"})

;;; Palauttaa hierarkian
(select-hierarchy db)
;; => ({:tieteellinen "Cinclus cinclus", :nimi "Koskikara", :heimo-tieteellinen "Cinclus", :heimo-nimi "Koskikarat"})

;;; Ladataan enemmän dataa tietokantaan
(def heimoja [["Tiaiset" "Paridae"] ["Peipot" "Fringillidae"]])

(map (fn [[nimi latin]]
       (insert-heimo db {:tieteellinen latin :nimi nimi})) heimoja)
;; => (({:id 2, :tieteellinen "Paridae", :nimi "Tiaiset"}) ({:id 3, :tieteellinen "Fringillidae", :nimi "Peipot"}))

(def tiaisia [["Hömötiainen" "Poecile montanus"]
              ["Lapintiainen" "Poecile cinctus"]
              ["Kuusitiainen" "Periparus ater"]
              ["Talitiainen" "Parus major"]
              ["Sinitiainen" "Cyanistes caeruleus"]
              ["Töyhtötiainen" "Lophophanes cristatus"]])

(map (fn [[nimi latin]]
       (insert-laji db {:heimo-id 2 :tieteellinen latin :nimi nimi})) tiaisia)
;; => (({:id 2, :tieteellinen "Poecile montanus", :nimi "Hömötiainen", :heimo-id 2}) ({:id 3, :tieteellinen "Poecile cinctus", :nimi "Lapintiainen", :heimo-id 2}) ({:id 4, :tieteellinen "Periparus ater", :nimi "Kuusitiainen", :heimo-id 2}) ({:id 5, :tieteellinen "Parus major", :nimi "Talitiainen", :heimo-id 2}) ({:id 6, :tieteellinen "Cyanistes caeruleus", :nimi "Sinitiainen", :heimo-id 2}) ({:id 7, :tieteellinen "Lophophanes cristatus", :nimi "Töyhtötiainen", :heimo-id 2}))

(->> (select-hierarchy db)
     (reduce (fn [m {:keys [heimo-tieteellinen
                            heimo-nimi
                            tieteellinen
                            nimi]}]
               (update m {:tieteellinen heimo-tieteellinen
                          :nimi heimo-nimi} conj {:tieteellinen tieteellinen
                                                  :nimi nimi})) {}))
