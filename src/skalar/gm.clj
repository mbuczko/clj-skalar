(ns skalar.gm)

(defn options
  [opts]
  (reduce
   (fn [reduced [k v]]
     (if (or (and (boolean? v) (not v))
             (= :output-directory k))
       reduced
       (str reduced " -" (name k) " " (when (string? v) v))))
   ""
   opts))

(defn crop
  ([coords]
   (when (vector? coords)
     (apply crop coords)))
  ([x y width height]
   ["crop" (str width "x" height "+" x "+" y)]))

(defn resize
  [size & opts]
  (let [resize-opts (set opts)]
    ["resize" (str size
                   (when (:exact resize-opts) "!")
                   (when (:no-profiles resize-opts) " +profile \"*\"")
                   (when (:thumbnail resize-opts) " -thumbnail"))]))
