(set-env!
 :source-paths #{"src"}
 :dependencies '[[org.clojure/clojure "1.8.0" :scope "provided"]
                 [adzerk/bootlaces "0.1.13" :scope "test"]
                 [com.taoensso/nippy "2.13.0"]
                 [funcool/promesa "1.9.0"]
                 [me.raynes/conch "0.8.0"]])

;; to check the newest versions:
;; boot -d boot-deps ancient

(def +version+ "1.0.0")
(require '[adzerk.bootlaces :refer :all])

(bootlaces! +version+)

(task-options!
 pom {:project 'defunkt/skalar
      :version +version+
      :description "Image resizing library"
      :url "https://github.com/mbuczko/skalar"
      :scm {:url "https://github.com/mbuczko/skalar"}})
