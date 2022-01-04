(ns terminal-app.navigate
  (:require [clojure.java.shell :as sh])
  (:require [clojure.string :as string])
  (:require [clojure.java.io :as io]))

(defn sort-file-list [files]
  (vec (sort-by (juxt :nodir? :hidden? :name) files)))

(defn generate-file-list [file]
  (sort-file-list (mapv (fn [f] {:obj f
                                 :name (.getName f)
                                 :nodir? (not (.isDirectory f))
                                 :hidden? (.isHidden f)})
                        (.listFiles file))))

(defn get-sel-file [dir]
  (get-in dir [:files (dir :sel) :obj]))

(defn get-file-content [file]
  (let [file-info ((sh/sh "file" file) :out)]
    (if (.contains file-info " text")
      (with-open [rdr (io/reader file)]
        (vec (take 100 (line-seq rdr))))
      [(string/join " " (drop 1 (string/split (string/trim (str file-info)) #" ")))])))

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
     :content (get-file-content (.getAbsolutePath file))}))

(defn update-prev-state [state]
  (assoc state
         :prev-dir
         (get-prev-state (get-sel-file (state :dir)))))

(defn update-top-bar [state]
  (let [dir (state :dir)]
    (update state :top-bar assoc
            :path (.getAbsolutePath (dir :file))
            :file (get-in dir [:files (dir :sel) :name]))))

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
                :sel 0
                :scroll-pos 0}
     :prev-dir (get-prev-state (get-in files [0 :obj]))
     :top-bar  {:path path
                :file (get-in files [0 :name])}
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
      (update-top-bar st)
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
       (update-top-bar st)
       (if (< (get-in st [:dir :sel])
              (get-in st [:dir :scroll-pos]))
         (update-in st [:dir :scroll-pos] dec)
         st))
     state))

(defn folder-up [state]
  (if (get-in state [:par-dir :file])
    (as-> state st
      (assoc st :dir (state :par-dir))
      (update-prev-state st)
      (update-top-bar st)
      (if-let [par-file (.getParentFile (get-in st [:par-dir :file]))]
        (let [name      (.getName (get-in st[:par-dir :file]))
              par-files (generate-file-list par-file)
              par-sel   (.indexOf (mapv (fn [f] (f :name)) par-files) name)]
          (update st :par-dir assoc
                  :file par-file
                  :files par-files
                  :sel par-sel
                  :scroll-pos (max 0 (- par-sel
                                        (get-in st [:layout :list-height])
                                        -1))))
        (update st :par-dir assoc
                :file nil
                :files []
                :sel -1
                :scroll-pos 0)))
    state))

(defn open-file [file]
  nil)

(defn folder-down [state]
  (let [file (get-sel-file (state :dir))]
    (if (.isDirectory file)
      (-> state
          (assoc :par-dir (state :dir))
          (assoc :dir (state :prev-dir))
          (update-prev-state)
          (update-top-bar))
      (do (open-file file)
          state))))
