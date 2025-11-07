(ns multittt.hiccuper
(:require ["npm:jsdom$default" :as jsdom]))

(defrecord Fragment [children])

(defn fragment? [x]
  (instance? Fragment x))

(defn append-child! [^js el child]
  (if (fragment? child)
    (doseq [child (:children child)
            :when (some? child)]
      (append-child! el child))
    (.append el child)))

(defn hiccup->element [hv ^js doc]
  (if (vector? hv)
    (let [[tag attrs-or-child & children] hv
          attrs (when (map? attrs-or-child) attrs-or-child)
          children (cond->> children (nil? attrs) (cons attrs-or-child))]
      (if (= tag :<>)
        (do
          (assert (nil? attrs))
          (->Fragment (mapv #(hiccup->element % doc) children)))
        (let [el (.createElement doc (name tag))]
          (doseq [[attr val] attrs]
            (.setAttribute el (name attr) (str val)))
          (doseq [child children
                  :let [child-el (hiccup->element child doc)]
                  :when child-el]
            (append-child! el child-el))
          el)))
    hv))

(defn hiccup->document [hv]
  (let [jsd (jsdom/JSDOM. "<!doctype html>")
        window jsd.window
        document window.document
        doc (.createHTMLDocument document.implementation nil nil)
        _ (println doc)
        el (hiccup->element hv doc)
        _ (println el)
        ]
    (when el
      (append-child! doc.body el))
    doc.body.innerHTML))
