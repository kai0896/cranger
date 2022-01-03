(ns terminal-app.navigate
  (:require [clojure.java.io :as io]))

(defn generate-file-list [file]
  (vec (sort (mapv str (.list file)))))

(defn init-state [path]
  (let [file (io/file path)
        par-file (.getParentFile file)
        par-files (generate-file-list par-file)]
    {:file file
     :files (generate-file-list file)
     :sel 0
     :sel-prev nil
     :scroll-pos 0
     :par-dir {:file par-file
               :files par-files
               :sel (.indexOf par-files (.getName file))
               :scroll-pos 0}
     :prev-dir {}
     :layout {:size []
              :top-bar-height 1
              :bottom-bar-height 1
              :col1-percent 0.2
              :col2-percent 0.6}}))

(defn sel-down [state]
  (if (< (state :sel) (- (count (state :files)) 1))
    (as-> state st
      (update st :sel inc)
      (if (>= (+ (st :sel) (st :scroll-pos)) (get-in st [:layout :list-height]))
        (update st :scroll-pos inc)
        st))
    state))

(defn sel-up [state]
  (if (> (state :sel) 0)
     (as-> state st
       (update st :sel dec)
       (if (< (st :sel) (st :scroll-pos))
         (update st :scroll-pos dec)
         st))
     state))

(defn folder-up [state]
  (let [name (.getName (get-in state [:par-dir :file]))
        par-file (.getParentFile (get-in state [:par-dir :file]))
        par-files (generate-file-list par-file)
        par-sel (.indexOf par-files name)]
    (-> state
        (assoc :file (get-in state [:par-dir :file]))
        (assoc :files (get-in state [:par-dir :files]))
        (assoc :sel (get-in state [:par-dir :sel]))
        (assoc :scroll-pos (get-in state [:par-dir :scroll-pos]))
        (assoc-in [:par-dir :file] par-file)
        (assoc-in [:par-dir :files] par-files)
        (assoc-in [:par-dir :sel] par-sel)
        (assoc-in [:par-dir :scroll-pos] (max 0 (- par-sel (get-in state [:layout :list-height]) -1))))))

(defn folder-down [state]
  (let [file (io/file (str (.getAbsolutePath (state :file))
                           "/"
                           ((state :files) (state :sel))))
        files (generate-file-list file)]
    (-> state
      (assoc-in [:par-dir :file] (state :file))
      (assoc-in [:par-dir :files] (state :files))
      (assoc-in [:par-dir :sel] (state :sel))
      (assoc-in [:par-dir :scroll-pos] (state :scroll-pos))
      (assoc :file file)
      (assoc :files files)
      (assoc :sel 0)
      (assoc :scroll-pos 0))))
