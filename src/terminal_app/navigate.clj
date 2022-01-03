(ns terminal-app.navigate
  (:require [clojure.java.io :as io]))

(defn generate-file-list [file]
  (mapv (fn [f] {:obj f
                 :name (.getName f)
                 :dir? (.isDirectory f)})
        (.listFiles file)))

(defn get-sel-file [dir]
  (get-in dir [:files (dir :sel) :obj]))

(defn get-prev-state [file]
  (if (.isDirectory file)
    {:file file
     :files (generate-file-list file)
     :sel 0
     :scroll-pos 0
     :content nil}
    {:file nil
     :filess nil
     :sel nil
     :scroll-pos nil
     :content "this is a file"}))

(defn update-prev-state [state]
  (assoc state
         :prev-dir
         (get-prev-state (get-sel-file (state :dir)))))

(defn init-state [path]
  (let [file (io/file path)
        files (generate-file-list file)
        par-file (.getParentFile file)
        par-files (generate-file-list par-file)]
    {:dir      {:file file
                :files files
                :sel 0
                :scroll-pos 0}
     :par-dir  {:file par-file
                :files par-files
                :sel (.indexOf par-files (.getName file))
                :scroll-pos 0}
     :prev-dir (get-prev-state (get-in files [0 :obj]))
     :layout   {:size []
                :top-bar-height 1
                :bottom-bar-height 1
                :col1-percent 0.2
                :col2-percent 0.6}}))

(defn sel-down [state]
  (if (< (get-in state [:dir :sel])
         (- (count (get-in state [:dir :files])) 1))
    (as-> state st
      (update-in st [:dir :sel] inc)
      (update-prev-state st)
      (if (>= (+ (get-in st [:dir :sel]) (get-in st [:dir :scroll-pos]))
              (get-in st [:layout :list-height]))
        (update-in st [:dir :scroll-pos] inc)
        st))
    state))

(defn sel-up [state]
  (if (> (get-in state [:dir :sel]) 0)
     (as-> state st
       (update-in st [:dir :sel] dec)
       (update-prev-state st)
       (if (< (get-in st [:dir :sel])
              (get-in st [:dir :scroll-pos]))
         (update-in st [:dir :scroll-pos] dec)
         st))
     state))

(defn folder-up [state]
  (let [name (.getName (get-in state [:par-dir :file]))
        par-file (.getParentFile (get-in state [:par-dir :file]))
        par-files (generate-file-list par-file)
        par-sel (.indexOf (mapv (fn [f] (f :name)) par-files) name)]
    (-> state
        (assoc :dir (state :par-dir))
        (update-prev-state)
        (assoc-in [:par-dir :file] par-file)
        (assoc-in [:par-dir :files] par-files)
        (assoc-in [:par-dir :sel] par-sel)
        (assoc-in [:par-dir :scroll-pos] (max 0 (- par-sel (get-in state [:layout :list-height]) -1))))))

(defn open-file [file]
  (print "TODO open file"))

(defn folder-down [state]
  (let [file (get-sel-file (state :dir))]
    (if (.isDirectory file)
      (-> state
          (assoc :par-dir (state :dir))
          (assoc :dir (state :prev-dir))
          (update-prev-state))
      (do (open-file file)
          state))))
