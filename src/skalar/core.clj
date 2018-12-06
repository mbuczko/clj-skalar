(ns skalar.core
  (:require [clojure.java.io :as io]
            [skalar.pool :as pool]))

(def default-pool
  (delay (pool/create-pool 3 6 8)))

(defn- reduce-params
  [init params]
  (reduce (fn [out param]
            (if (string? param)
              (str out param)
              (str out " -" (first param) " " (second param))))
          init
          params))

(defn file-from-url [url]
  (let [tmpfile (java.io.File/createTempFile "img-" nil)]
    (with-open [in (io/input-stream url)
                out (io/output-stream tmpfile)]
      (io/copy in out))
    tmpfile))

(defn convert
  "Converts input file into `:output` based on a collection of processing commands.
  If output is not provided, input file get overriden.

  Returns a promise which resolves to either transformed image file
  or (in case of failure) to an error."

  [file & {:keys [output processing pool]}]
  {:pre [(or (nil? output) (string? output))]}
  (pool/send-cmd
   (or pool @default-pool)
   (or output (str file))
   (reduce-params (str "convert " file) processing)))
