(ns terminal-app.navigate
  (:require [clojure.java.io :as io]))

(defn generate-file-list [file]
  (mapv str (.list file)))

(defn init-state [path]
  (let [file (io/file path)
        par-file (.getParentFile file)]
   {:file file
    :files (generate-file-list file)
    :sel 0
    :sel-prev nil
    :scroll-pos 0
    :par-dir {:file par-file
              :files (generate-file-list par-file)
              :sel nil}
    :prev-dir {}
    :layout {:size []
             :top-bar-height 1
             :bottom-bar-height 1
             :col1-percent 0.2
             :col2-percent 0.6}}))

(defn sel-down [state]
  (update state
          :sel
          #(min (- (count (state :files)) 1) (inc %))))

(defn sel-up [state]
  (update state
          :sel
          #(max 0 (dec %))))

(defn folder-up [state]
  (let [name (.getName (state :file))]
    (as-> state st
        (update st :file #(.getParentFile %))
        (assoc st :files (generate-file-list (st :file)))
        (assoc st :sel (.indexOf (st :files) name)))))

(defn folder-down [state]
  (as-> state st
    (assoc st :file (io/file (str (.getAbsolutePath (state :file))
                                  "/"
                                  ((state :files) (state :sel)))))
      (assoc st :files (generate-file-list (st :file)))
      (assoc st :sel 0)))
