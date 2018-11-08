(ns skalar.pool
  (:require [me.raynes.conch.low-level :as sh]
            [promesa.core :as p]))

(declare create-session close-session)

(def temp-dir-attrs
  (into-array java.nio.file.attribute.FileAttribute []))

(defprotocol Poolable
  (reserve-session [this] "reserves session from pool adding one if capacity has been reached")
  (release-session [this session] "releases session potentially removing it from pool"))

(defrecord Pool [base capacity max sessions working-dir]
  Poolable

  (reserve-session [this]
    (let [session  (apply min-key (comp deref :attached) sessions)
          created  (count sessions)
          attached (:attached session)]

      (if (or (= created max)
              (< @attached capacity))
        (do (swap! attached inc) session)
        (first
         (swap! sessions conj (create-session (inc created) 1 working-dir))))))

  (release-session [this session]
    (let [attached (swap! (:attached session) dec)]
      (if (or (< attached 0)
              (and (= attached 0)
                   (> (count sessions) base)))
        (let [id (:id (close-session session))]
          (swap! sessions (filter #(not (= (:id %1) id)) sessions)))
        sessions))))

(defn create-proc [workdir]
  (let [proc (sh/proc "gm" "batch" "-feedback" "on" :dir workdir)]
    (sh/read-line proc :out)
    proc))

(defn create-pool [base capacity max & [workdir]]
  (let [wdir (or workdir ".")
        iter (take base (iterate inc 1))
        sessions (doall (map #(create-session % 0 wdir) iter))]
    (->Pool base capacity max sessions wdir)))

(defn shutdown-pool [pool]
  (doseq [session @(:sessions pool)]
    (.release-session pool session)))

(defn close-session [session]
  (when-let [proc @(:process session)]
    (sh/done proc)
    (sh/destroy proc)
    session))

(defn create-session [n attached workdir]
  {:id       (str "gm-" n)
   :attached (atom attached)
   :process  (agent (create-proc workdir))})

(defn send-cmd [pool cmd output]
  (let [session (reserve-session pool)
        process (:process session)
        workdir (:working-dir pool)]

    (p/promise
     (fn [resolve reject]
       (send-off process
                 (fn [proc args]
                   (try

                     ;; process a command
                     (sh/feed-from-string proc args)

                     ;; gather a feedback from session
                     (let [feedback (sh/read-line proc :out)]
                       (release-session pool session)

                       (if (= "FAIL" feedback)
                         (reject (Exception. "Error while processing. Possibly not a valid image."))
                         (resolve (->> (into-array String [output])
                                       (java.nio.file.Paths/get workdir)
                                       (.toAbsolutePath)
                                       (.toString)))))
                     (catch Exception e
                       (reject e)))
                   proc)
                 cmd)))))
