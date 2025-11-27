# mazeru

https://en.wiktionary.org/wiki/%E6%B7%B7%E3%81%9C%E3%82%8B

  deno -A jsr:@babashka/nbb -m --classpath src multittt.state


# watching files

ls src/multittt/* | entr -r deno run server


# start nrepl

 clojure -T:nrebel :port 1337

# start browser repl

Settings for rebel

````
greg@gbl-medion:~/projects/multi_ttt$ cat ~/.clojure/rebel_readline.edn
{:key-map :viins
 :key-bindings {:viins [["^M" :clojure-force-accept-line
                         "^J" :clojure-force-accept-line]]}}
```
  

./browser-repl.bb

clojure -T:nrebel :port 1339

### reload

```
 user=> (require '[multittt.frontend] :reload)
```

you need to be in an other namespace (maybe even in user) than the one your reloading for this to work 

(multittt.stream/send-signal (last (last @multittt.server/sessions)) #js{:tablevalue 'def'})
(require '[multittt.server] :reload) (require '[multittt.stream] :reload) (require 'multittt.frontend :reload)
(multittt.stream/transfer (last (last @multittt.server/sessions)) "topelement" multittt.frontend/alpinesample)
user=> (multittt.stream/send-signal (last (last @multittt.server/sessions)) #js{:tablevalue "something even newer"})
signals {"tablevalue":"something even newer"}

true
user=> (multittt.stream/transfer (last (last @multittt.server/sessions)) "topelement" [:div "from repl"])
renderdata {"elem":"topelement","hic":"[:div \"from repl\"]\n"}
