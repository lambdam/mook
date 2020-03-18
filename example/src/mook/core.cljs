(ns mook.core
  (:require [mook.command :as c]
            [mook.hooks :as h]))

(def command>> c/command>>)

(def register-store! h/register-store!)
