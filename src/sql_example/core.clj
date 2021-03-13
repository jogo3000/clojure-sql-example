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

;;; Palauttaa kaikki lajie
(select-all-laji db)

;;; Palauttaa hierarkian
(select-hierarchy db)
;; => ({:tieteellinen "Cinclus cinclus", :nimi "Koskikara", :heimo-tieteellinen "Cinclus", :heimo-nimi "Koskikarat"})


;;; Ladataan enemmän dataa tietokantaan
(def heimoja [["Tiaiset" "Paridae"] ["Peipot" "Fringillidae"]])

(run! (fn [[nimi latin]]
       (insert-heimo db {:tieteellinen latin :nimi nimi})) heimoja)
;; => (({:id 2, :tieteellinen "Paridae", :nimi "Tiaiset"}) ({:id 3, :tieteellinen "Fringillidae", :nimi "Peipot"}))

(def tiaisia [["Hömötiainen" "Poecile montanus"]
              ["Lapintiainen" "Poecile cinctus"]
              ["Kuusitiainen" "Periparus ater"]
              ["Talitiainen" "Parus major"]
              ["Sinitiainen" "Cyanistes caeruleus"]
              ["Töyhtötiainen" "Lophophanes cristatus"]])

(def peippoja [["Urpiainen" "Acanthis flammea"]
               ["Tundraurpiainen" "Acanthis hornemanni"]
               ["Tikli" "Carduelis carduelis"]
               ["Punavarpunen" "Carpodacus erythrinus"]
               ["Viherpeippo" "Chloris chloris"]
               ["Nokkavarpunen" "Coccothraustes coccothraustes"]
               ["Peippo" "Fringilla coelebs"]
               ["Järripeippo" "Fringilla montifringilla"]
               ["Hemppo" "Linaria cannabina"]
               ["Vuorihemppo" "Linaria flavirostris"]
               ["Pikkukäpylintu" "Loxia curvirostra"]
               ["Kirjosiipikäpylintu" "Loxia leucoptera"]
               ["Isokäpylintu" "Loxia pytyopsittacus"]
               ["Taviokuurna" "Pinicola enucleator"]
               ["Punatulkku" "Pyrrhula pyrrhula"]
               ["Keltahemppo" "Serinus serinus"]
               ["Vihervarpunen" "Spinus spinus"]])

(run! (fn [[nimi latin]]
        (insert-laji db {:heimo-id 2 :tieteellinen latin :nimi nimi})) tiaisia)

(run! (fn [[nimi latin]]
        (insert-laji db {:heimo-id 3 :tieteellinen latin :nimi nimi})) peippoja)

(->> (select-hierarchy db)
     (reduce (fn [m {:keys [heimo-tieteellinen
                            heimo-nimi
                            tieteellinen
                            nimi]}]
               (update m {:tieteellinen heimo-tieteellinen
                          :nimi heimo-nimi} conj {:tieteellinen tieteellinen
                                                  :nimi nimi})) {}))


;;; Helpompikin tapa syöttää lajit
(def taksonomia {["Cinclus" "Koskikarat"]
                 [["Cinclus cinclus" "Koskikara"]]

                 ["Tiaiset" "Paridae"]
                 tiaisia

                 ["Peippoja" "Fringillidae"]
                 peippoja})

(jdbc/with-db-transaction [tx db]
  (->> taksonomia
       (run! (fn [[heimo lajit]]
               (let [heimo-id (-> (insert-heimo tx {:tieteellinen (first heimo) :nimi (second heimo)}) :id)]
                 (assert heimo-id)
                 (run! (fn [[latin nimi]]
                         (insert-laji tx {:tieteellinen latin :nimi nimi :heimo-id heimo-id})) lajit))))))
