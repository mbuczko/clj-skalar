(ns skalar.core
  (:require [clojure.java.io :as io]
            [skalar.gm :as gm]
            [skalar.pool :as pool]
            [clojure.string :as str]))

(def default-pool (memoize pool/create-pool))

(defn file-from-url [url]
  (let [tmpfile (java.io.File/createTempFile "img-" nil)]
    (with-open [in (io/input-stream url)
                out (io/output-stream tmpfile)]
      (io/copy in out))
    tmpfile))

(defn convert
  "Converts image according to given command.
  Returns a promise which resolves to either transformed image data
  or (in case of failure) to an error."

  [file & {:keys [to processing pool]}]
  {:pre [(string? to)]}
  (let [pool (or pool (default-pool 3 6 8))
        init (str "convert " file)
        cmd  (str (reduce (fn [out proc]
                            (if (string? proc)
                              (str out proc)
                              (str out " -" (first proc) " " (second proc))))
                          init
                          processing)
                  " " to "\n")]

    (pool/send-cmd pool cmd to)))
