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

(defn init-state [path scr]
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
                :col2-percent 0.6}
     :scr scr}))

(defn adjust-scroll-pos [state]
  (let [sel (get-in state [:dir :sel])
        height (get-in state [:layout :list-height])]
    (update-in state
               [:dir :scroll-pos]
               #(let [outside-bottom (< (- (+ % height) sel 1) 0)
                      outside-top (< sel %)]
                   (cond outside-bottom (- sel height -1)
                         outside-top sel
                         :else %)))))

(adjust-scroll-pos {:dir    {:sel         4
                             :scroll-pos  4}
                    :layout {:list-height 10}})

(defn sel-down [state]
  (if (< (get-in state [:dir :sel])
         (- (count (get-in state [:dir :files])) 1))
    (as-> state st
      (update-in st [:dir :sel] inc)
      (update-prev-state st)
      (update-top-bar st)
      (adjust-scroll-pos st)
      ;; (if (>= (+ (get-in st [:dir :sel]) (get-in st [:dir :scroll-pos]))
      ;;         (get-in st [:layout :list-height]))
      ;;   (update-in st [:dir :scroll-pos] inc)
      ;;   st)
      )
    state))

(defn sel-up [state]
  (if (> (get-in state [:dir :sel]) 0)
     (as-> state st
       (update-in st [:dir :sel] dec)
       (update-prev-state st)
       (update-top-bar st)
       (adjust-scroll-pos st)
       ;; (if (< (get-in st [:dir :sel])
       ;;        (get-in st [:dir :scroll-pos]))
       ;;   (update-in st [:dir :scroll-pos] dec)
       ;;   st)
         )
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

(defn search-res-sel [state fn-pos fn-comp]
  (let [dir (state :dir)]
    (if (not-empty (dir :search-res))
      (-> state
          (update-in [:dir :sel]
                     #(if-let [new-sel (fn-pos (for [i (dir :search-res)
                                                     :when (fn-comp i %)] i))]
                        new-sel
                        (fn-pos (dir :search-res))))
          (adjust-scroll-pos))
      state)))

(defn search-res-down [state]
  (search-res-sel state first >))

(defn search-res-up [state]
  (search-res-sel state last <))

(defn search-res-reset [state]
  (assoc-in state [:dir :search-res] []))

(defn update-search-results [state query]
  (if (not-empty query)
    (let [files (get-in state [:dir :files])
          search-res (vec (for [i (range (count files))
                                :let [name (get-in files [i :name])]
                                :when (.contains name (apply str query))]
                            i))]
      (-> state
          (assoc-in [:dir :search-res] search-res)
          (search-res-down)))
    (assoc-in state [:dir :search-res] [])))
