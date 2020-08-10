(ns mook.react
  (:require #_["react" :as react]
            #_["react-dom" :as react-dom]
            [clojure.string :as str]
            #?(:clj [clojure.spec.alpha :as s])
            #?(:cljs [react :as react])
            #?(:cljs [cljs-bean.core :as b]))
  #?(:cljs
     (:require-macros [mook.react :refer [def-elems]])))

#?(:cljs
   (do

     (def ^:private createElement
       react/createElement)

     (defn create-element
       ([comp]
        (createElement comp nil))
       ([comp opts]
        (if (map? opts)
          (createElement comp (b/->js opts))
          (createElement comp nil opts)))
       ([comp opts el1]
        (if (map? opts)
          (createElement comp (b/->js opts) el1)
          (createElement comp nil opts el1)))
       ([comp opts el1 el2]
        (if (map? opts)
          (createElement comp (b/->js opts) el1 el2)
          (createElement comp nil opts el1 el2)))
       ([comp opts el1 el2 el3]
        (if (map? opts)
          (createElement comp (b/->js opts) el1 el2 el3)
          (createElement comp nil opts el1 el2 el3)))
       ([comp opts el1 el2 el3 el4]
        (if (map? opts)
          (createElement comp (b/->js opts) el1 el2 el3 el4)
          (createElement comp nil opts el1 el2 el3 el4)))
       ([comp opts el1 el2 el3 el4 el5]
        (if (map? opts)
          (createElement comp (b/->js opts) el1 el2 el3 el4 el5)
          (createElement comp nil opts el1 el2 el3 el4 el5)))
       ([comp opts el1 el2 el3 el4 el5 el6]
        (if (map? opts)
          (createElement comp (b/->js opts) el1 el2 el3 el4 el5 el6)
          (createElement comp nil opts el1 el2 el3 el4 el5 el6)))
       ([comp opts el1 el2 el3 el4 el5 el6 el7]
        (if (map? opts)
          (createElement comp (b/->js opts) el1 el2 el3 el4 el5 el6 el7)
          (createElement comp nil opts el1 el2 el3 el4 el5 el6 el7)))
       ([comp opts el1 el2 el3 el4 el5 el6 el7 el8]
        (if (map? opts)
          (createElement comp (b/->js opts) el1 el2 el3 el4 el5 el6 el7 el8)
          (createElement comp nil opts el1 el2 el3 el4 el5 el6 el7 el8)))
       ([comp opts el1 el2 el3 el4 el5 el6 el7 el8 el9]
        (if (map? opts)
          (createElement comp (b/->js opts) el1 el2 el3 el4 el5 el6 el7 el8 el9)
          (createElement comp nil opts el1 el2 el3 el4 el5 el6 el7 el8 el9)))
       ([comp opts el1 el2 el3 el4 el5 el6 el7 el8 el9 el10]
        (if (map? opts)
          (createElement comp (b/->js opts) el1 el2 el3 el4 el5 el6 el7 el8 el9 el10)
          (createElement comp nil opts el1 el2 el3 el4 el5 el6 el7 el8 el9 el10)))
       ([comp opts el1 el2 el3 el4 el5 el6 el7 el8 el9 el10 & children]
        (if (map? opts)
          (apply createElement comp (b/->js opts) el1 el2 el3 el4 el5 el6 el7 el8 el9 el10 children)
          (apply createElement comp nil opts el1 el2 el3 el4 el5 el6 el7 el8 el9 el10 children))))

     (def fragment
       (partial create-element react/Fragment))

     (defn create-context [default-value]
       (let [context (react/createContext default-value)
             provider-class (.-Provider context)]
         {::context context
          ::provider-class provider-class
          ::provider (partial create-element provider-class)
          ::consumer (.-Consumer context)}))

     (def use-state
       react/useState)

     (def use-effect
       react/useEffect)

     (def use-context
       react/useContext)

     (def use-ref
       react/useRef)

     ;; Utility

     (defn classes [prop-map]
       (->> (reduce (fn [acc [prop pred?]]
                      (if pred?
                        (conj acc (name prop))
                        acc))
                    []
                    prop-map)
            (str/join " ")))

     ))

#?(:clj
   (do

     (defmacro def-elems [names]
       (s/assert (s/map-of symbol? any?) names)
       `(do ~@(for [[sym# component#] names]
                `(def ~sym#
                   (partial mook.react/create-element ~component#)))))

     (defmacro def-html-elems! []
       ;; https://gist.github.com/bramus/a9c1bad426e6f4fd9af0f19ecb2e24a3
       `(def-elems {~'a "a"
                    ~'abbr "abbr"
                    ~'acronym "acronym"
                    ~'address "address"
                    ~'applet "applet"
                    ~'area "area"
                    ~'article "article"
                    ~'aside "aside"
                    ~'audio "audio"
                    ~'b "b"
                    ~'base "base"
                    ~'basefont "basefont"
                    ~'bdi "bdi"
                    ~'bdo "bdo"
                    ~'bgsound "bgsound"
                    ~'big "big"
                    ~'blink "blink"
                    ~'blockquote "blockquote"
                    ~'body "body"
                    ~'br "br"
                    ~'button "button"
                    ~'canvas "canvas"
                    ~'caption "caption"
                    ~'center "center"
                    ~'cite "cite"
                    ~'code "code"
                    ~'col "col"
                    ~'colgroup "colgroup"
                    ~'command "command"
                    ~'content "content"
                    ~'data "data"
                    ~'datalist "datalist"
                    ~'dd "dd"
                    ~'del "del"
                    ~'details "details"
                    ~'dfn "dfn"
                    ~'dialog "dialog"
                    ~'dir "dir"
                    ~'div "div"
                    ~'dl "dl"
                    ~'dt "dt"
                    ~'element "element"
                    ~'em "em"
                    ~'embed "embed"
                    ~'fieldset "fieldset"
                    ~'figcaption "figcaption"
                    ~'figure "figure"
                    ~'font "font"
                    ~'footer "footer"
                    ~'form "form"
                    ~'frame "frame"
                    ~'frameset "frameset"
                    ~'h1 "h1"
                    ~'h2 "h2"
                    ~'h3 "h3"
                    ~'h4 "h4"
                    ~'h5 "h5"
                    ~'h6 "h6"
                    ~'head "head"
                    ~'header "header"
                    ~'hgroup "hgroup"
                    ~'hr "hr"
                    ~'html "html"
                    ~'i "i"
                    ~'iframe "iframe"
                    ~'image "image"
                    ~'img "img"
                    ~'input "input"
                    ~'ins "ins"
                    ~'isindex "isindex"
                    ~'kbd "kbd"
                    ~'keygen "keygen"
                    ~'label "label"
                    ~'legend "legend"
                    ~'li "li"
                    ~'link "link"
                    ~'listing "listing"
                    ~'main "main"
                    ~'map "map"
                    ~'mark "mark"
                    ~'marquee "marquee"
                    ~'menu "menu"
                    ~'menuitem "menuitem"
                    ~'meta "meta"
                    ~'meter "meter"
                    ~'multicol "multicol"
                    ~'nav "nav"
                    ~'nextid "nextid"
                    ~'nobr "nobr"
                    ~'noembed "noembed"
                    ~'noframes "noframes"
                    ~'noscript "noscript"
                    ~'object "object"
                    ~'ol "ol"
                    ~'optgroup "optgroup"
                    ~'option "option"
                    ~'output "output"
                    ~'p "p"
                    ~'param "param"
                    ~'picture "picture"
                    ~'plaintext "plaintext"
                    ~'pre "pre"
                    ~'progress "progress"
                    ~'q "q"
                    ~'rb "rb"
                    ~'rp "rp"
                    ~'rt "rt"
                    ~'rtc "rtc"
                    ~'ruby "ruby"
                    ~'s "s"
                    ~'samp "samp"
                    ~'script "script"
                    ~'section "section"
                    ~'select "select"
                    ~'shadow "shadow"
                    ~'slot "slot"
                    ~'small "small"
                    ~'source "source"
                    ~'spacer "spacer"
                    ~'span "span"
                    ~'strike "strike"
                    ~'strong "strong"
                    ~'style "style"
                    ~'sub "sub"
                    ~'summary "summary"
                    ~'sup "sup"
                    ~'table "table"
                    ~'tbody "tbody"
                    ~'td "td"
                    ~'template "template"
                    ~'textarea "textarea"
                    ~'tfoot "tfoot"
                    ~'th "th"
                    ~'thead "thead"
                    ~'time "time"
                    ~'title "title"
                    ~'tr "tr"
                    ~'track "track"
                    ~'tt "tt"
                    ~'u "u"
                    ~'ul "ul"
                    ~'var "var"
                    ~'video "video"
                    ~'wbr "wbr"
                    ~'xmp "xmp"

                    ;; svg
                    ~'circle "circle"
                    ~'clipPath "clipPath"
                    ~'ellipse "ellipse"
                    ~'g "g"
                    ~'line "line"
                    ~'mask "mask"
                    ~'path "path"
                    ~'pattern "pattern"
                    ~'polyline "polyline"
                    ~'rect "rect"
                    ~'svg "svg"
                    ~'text "text"
                    ~'defs "defs"
                    ~'linearGradient "linearGradient"
                    ~'polygon "polygon"
                    ~'radialGradient "radialGradient"
                    ~'stop "stop"
                    ~'tspan "tspan"
                    }))

     ))
