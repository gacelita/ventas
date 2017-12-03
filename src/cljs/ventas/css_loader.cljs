(ns ventas.css-loader
  "Based on https://github.com/mhallin/forest/blob/master/src/forest/runtime.cljs
   Maybe useful in the future for the plugin system")

(defonce known-stylesheets (atom {}))

(defn- make-style-element []
  (let [elem (js/document.createElement "link")
        head (js/document.querySelector "head")]
    (assert (some? head)
            "A head element must be present for styles to be inserted")
    (set! (.-rel elem) "stylesheet")
    (.appendChild head elem)
    elem))

(defn- update-style-element! [element contents]
  (set! (.-href element) contents))

(defn- insert-style-element! [style-id contents]
  (let [elem (make-style-element)]
    (update-style-element! elem contents)
    (swap! known-stylesheets assoc style-id elem)))

(defn update-stylesheet! [stylesheet]
  (assert (some? stylesheet))
  (let [{:keys [full-name href]} stylesheet]
    (if-let [existing (@known-stylesheets full-name)]
      (update-style-element! existing href)
      (insert-style-element! full-name href))))