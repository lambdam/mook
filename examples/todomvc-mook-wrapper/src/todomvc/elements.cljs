(ns todomvc.elements
  (:require [mook.react :as r :include-macros true]))

(r/def-elems
  {h1 "h1"
   h2 "h2"
   h3 "h3"
   h4 "h4"
   h5 "h5"
   h6 "h6"
   div "div"
   p "p"
   a "a"
   span "span"
   section "section"
   header "header"
   footer "footer"
   input "input"
   label "label"
   button "button"
   ul "ul"
   li "li"})
