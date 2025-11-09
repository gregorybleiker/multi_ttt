  deno -A jsr:@babashka/nbb -m --classpath src multittt.state


# watching files

ls src/multittt/* | entr -r deno run server


# start nrepl

 clojure -T:nrebel :port 1337

# start browser repl

./browser-repl.bb

clojure -T:nrebel :port 1339
