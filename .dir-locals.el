((clojurescript-mode
  (cider-clojure-cli-aliases . ":nbb")
  (eval . (cider-register-cljs-repl-type 'super-cljs "(do (foo) (bar))"))
  (cider-default-cljs-repl . super-cljs)))
