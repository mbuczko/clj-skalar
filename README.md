[![Clojars Project](https://img.shields.io/clojars/v/defunkt/skalar.svg)](https://clojars.org/defunkt/skalar)

# What is Skalar

Skalar is a humble attempt to build a fast [GraphicsMagick](http://www.graphicsmagick.org/) based image tranformation library.
There are already plenty of this kind of libraries all over the world and each one is special (at least in their authors' minds ;)

So, why _Skalar_?

Skalar achieves its robustness via underlaying pool of opened connections to GM sessions. Instead of creating new session with _gm_ executable
each time a request comes in, Skalar redirects request to first least-busy GM session (or more technically - the session with smallest number of assigned requests).
Once the number of assigned request on each opened sessions exceeds given capacity, Skalar adds one more session trying to balance incoming requests.

# Usage

```clojure
(require '[clojure.java.io :as io])
(require '[skalar.core :as sk])
(require '[skalar.gm :as gm])

(def input-file (io/file "photo.jpg"))

(sk/convert input-file 
            :output "photo.png"
            :processing [(gm/crop 10 10 100 100)
                         (gm/resize "200x200" :exact :no-profiles)
                         (gm/options {:auto-orient true})])
```

where `:processing` is a vector of [convert options](http://www.graphicsmagick.org/convert.html#conv-opti). Two of them (`-crop` and `-resize`) got a little helpers in
`skalar.gm` namespace to save people from remembering crazy GM syntax. All other options can be provided via `skalar.gm/options` function in keywordized form (without
prefixing -), so for example `{:auto-orient true}` gets translated to `-auto-orient`.

# Custom pool

So far default GraphicsMagick pool of processes (sessions) was used, but sometimes it's better to create a customized one. Each pool is defined by 4 parameters:

* base  : is a number of minimum sessions that need to be kept opened and ready for incoming requests.
* capacity : is a number of requests each session can handle. Once the number of session exceeds _capacity_ a new session is being added. 
* max : maximum number of sessions that can be created.

Default pool is defined with `base=3`, `capacity=6`, `max=8`, and that simply translates to:

_keep 3 opened sessions by default, consequently add new ones if number of requests for each existing session exceeds 6, but do not create more than 8 sessions in total_

Each additionally created session will be immediately closed and removed from pool when number of request drops down to 0 at some point in time.

Let's leave theory behind and create a custom pool:

``` clojure
(require '[skalar.pool :as sp])

(def my-pool
  (sp/create-pool 2 3 4 "/tmp"))
```

and provide it via `:pool` option:

``` clojure
(sk/convert (sk/file-from-url "https://images-assets.nasa.gov/image/PIA20912/PIA20912~orig.jpg")
            :output "kręcioł.png"
            :processing [(gm/resize "200x200")]
            :pool my-pool)
```

# Requirements

As mentioned Skalar bases on _gm_ executable which must be already installed to get this library running correctly.
To install _gm_ on Mac, simply use _brew_:

``` shell
brew install gm
```

GraphicsMagick is also available on [Alpine Linux](https://alpinelinux.org/), so [docker](https://www.docker.com/) is your friend if you don't want to pollute your OS.

