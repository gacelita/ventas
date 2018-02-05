(ns ventas.components.infinite-scroll
  "Shamelessly stolen from:
   https://github.com/nberger/reagent-infinite-scroll"
  (:require
   [reagent.core :as r]
   [ventas.utils :as utils]))

(defn- get-scroll-top []
  (if (exists? (.-pageYOffset js/window))
    (.-pageYOffset js/window)
    (.-scrollTop (or (.-documentElement js/document)
                     (.-parentNode (.-body js/document))
                     (.-body js/document)))))

(defn- get-el-top-position [node]
  (if (not node)
    0
    (+ (.-offsetTop node) (get-el-top-position (.-offsetParent node)))))

(defn- safe-component-mounted? [component]
  (try (boolean (r/dom-node component))
       (catch js/Object _ false)))

(defn infinite-scroll [props]
  (let [listener-fn (atom nil)
        detach-scroll-listener (fn []
                                 (when @listener-fn
                                   (.removeEventListener js/window "scroll" @listener-fn)
                                   (.removeEventListener js/window "resize" @listener-fn)
                                   (reset! listener-fn nil)))
        should-load-more? (fn [this]
                            (let [node (r/dom-node this)
                                  scroll-top (get-scroll-top)
                                  my-top (get-el-top-position node)
                                  threshold 50]
                              (< (- (+ my-top (.-offsetHeight node))
                                    scroll-top
                                    (.-innerHeight js/window))
                                 threshold)))
        scroll-listener (fn [this]
                          (when (safe-component-mounted? this)
                            (let [{:keys [load-fn can-show-more?]} (r/props this)]
                              (when (and can-show-more?
                                         (should-load-more? this))
                                (detach-scroll-listener)
                                (load-fn)))))
        debounced-scroll-listener (utils/debounce 200 scroll-listener)
        attach-scroll-listener (fn [this]
                                 (let [{:keys [can-show-more?]} (r/props this)]
                                   (when can-show-more?
                                     (when-not @listener-fn
                                       (reset! listener-fn (partial debounced-scroll-listener this))
                                       (.addEventListener js/window "scroll" @listener-fn)
                                       (.addEventListener js/window "resize" @listener-fn)))))]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (attach-scroll-listener this))
      :component-did-update
      (fn [this _]
        (attach-scroll-listener this))
      :component-will-unmount
      detach-scroll-listener
      :reagent-render
      (fn [props]
        [:div])})))
