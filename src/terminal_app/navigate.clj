(ns terminal-app.navigate
  (:require [clojure.java.shell :as sh])
  (:require [clojure.string :as string])
  (:require [clojure.java.io :as io]))

(defn sort-file-list [files]
  (vec (sort-by (juxt :nodir? :hidden?)
                (sort-by :name
                         String/CASE_INSENSITIVE_ORDER
                         files))))

(defn bytes-to-n [bytes n]
  (let [output (str (nth (iterate #(float (/ % 1024)) bytes) n))]
    (if (> (count output) 5)
      (subs output 0 5)
      output)))

(defn get-file-size-str [file]
  (if-not (.isDirectory file)
    (let [bytes (.length file)]
      (cond (< bytes 1000) (str bytes " B")
            (< bytes 1000000) (str (bytes-to-n bytes 1) " K")
            (< bytes 1000000000) (str (bytes-to-n bytes 2) " M")
            :else (str (bytes-to-n bytes 3) " G")))
    ""))

(defn generate-file-list! [file]
  (sort-file-list (mapv (fn [f] {:obj f
                                 :name (.getName f)
                                 :size (get-file-size-str f)
                                 :nodir? (not (.isDirectory f))
                                 :hidden? (.isHidden f)})
                        (.listFiles file))))

(defn get-file-content! [file-path]
  (let [file-info ((sh/sh "file" file-path) :out)]
    (if (.contains file-info " text")
      (with-open [rdr (io/reader file-path)]
        (vec (take 100 (line-seq rdr))))
      [(string/join " " (drop 1 (string/split (string/trim (str file-info)) #" ")))])))

(defn get-preview! [files]
  (mapv (fn [{file :obj}]
          (if (.isDirectory file)
            {:file file
             :files (generate-file-list! file)
             :sel 0
             :scroll-pos 0
             :content nil}
            {:file nil
             :filess nil
             :sel nil
             :scroll-pos nil
             :content (get-file-content! (.getAbsolutePath file))}))
        files))

(defn init-state! [path scr config]
  (let [file (io/file path)
        files (generate-file-list! file)
        par-file (.getParentFile file)
        par-files (generate-file-list! par-file)]
    {:mode     :prev
     :dir      {:file file
                :files files
                :sel 0
                :scroll-pos 0}
     :par-dir  {:file par-file
                :files par-files
                :sel 0
                :scroll-pos 0}
     :preview  (get-preview! files)
     :top-bar  {:path path
                :file (get-in files [0 :name])}
     :bottom-bar {}
     :layout   {:size []
                :top-bar-height 1
                :bottom-bar-height 1
                :col1-percent 0.2
                :col2-percent 0.6
                :colors (config :colors)}
     :keybinds (config :keybinds)
     :scr scr}))

(defn resize-screen [{:keys [layout] :as state} new-size]
  (update-in state [:layout] assoc
             :size new-size
             :list-height (- (new-size 1) (layout :top-bar-height) (layout :bottom-bar-height))
             :col1-char (int (* (new-size 0) (layout :col1-percent)))
             :col2-char (int (* (new-size 0) (layout :col2-percent)))))

(defn update-bars [{:keys [dir] :as state}]
    (update state :top-bar assoc
            :path (.getAbsolutePath (dir :file))
            :file (get-in dir [:files (dir :sel) :name]))
    ;; (update state :bottom-bar assoc)
    )

(defn adjust-scroll-pos [{{:keys [sel]} :dir
                          {:keys [list-height]} :layout
                          :as state}]
  (update-in state
             [:dir :scroll-pos]
             #(let [outside-bottom (< (- (+ % list-height) sel 1) 0)
                    outside-top (< sel %)]
                (cond outside-bottom (- sel list-height -1)
                      outside-top sel
                      :else %))))

(defn update-after-sel-change [state]
  (-> state
      (update-bars)
      (adjust-scroll-pos)))

(defn sel-down [{{:keys [sel files]} :dir
                 :as state}]
  (if (< sel (- (count files) 1))
    (-> state
        (update-in [:dir :sel] inc)
        (update-after-sel-change))
    state))

(defn sel-up [{{:keys [sel]} :dir
               :as state}]
  (if (> sel 0)
     (-> state
       (update-in [:dir :sel] dec)
       (update-after-sel-change))
     state))

(defn sel-top [state]
  (-> state
      (assoc-in [:dir :sel] 0)
      (update-after-sel-change)))

(defn sel-bottom [{{:keys [files]} :dir
               :as state}]
  (let [count-files (count files)]
    (-> state
        (assoc-in [:dir :sel] (- count-files 1))
        (update-after-sel-change))))

(defn folder-up! [{:keys [par-dir] :as state}]
  (if (par-dir :file)
    (as-> state st
      (assoc st :dir par-dir)
      (assoc st :preview (get-preview! (get-in st [:dir :files])))
      (update-bars st)
      (if-let [par-file (.getParentFile (get-in st [:par-dir :file]))]
        (let [name      (.getName (get-in st[:par-dir :file]))
              par-files (generate-file-list! par-file)
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

(defn open-file! [file]
  nil)

(defn folder-down! [{:keys [dir preview] :as state}]
  (let [file (get-in dir [:files (dir :sel) :obj])
        new-dir (preview (dir :sel))]
    (if (.isDirectory file)
      (-> state
          (assoc :par-dir dir)
          (assoc :dir new-dir)
          (assoc :preview (get-preview! (new-dir :files)))
          (update-bars))
      (do (open-file! file)
          state))))

(defn search-res-sel [{:keys [dir] :as state}
                      fn-pos fn-comp]
  (if (not-empty (dir :search-res))
    (-> state
        (update-in [:dir :sel]
                   #(if-let [new-sel (fn-pos (for [i (dir :search-res)
                                                   :when (fn-comp i %)] i))]
                      new-sel
                      (fn-pos (dir :search-res))))
        (update-after-sel-change))
    state))

(defn search-res-down [state]
  (search-res-sel state first >))

(defn search-res-up [state]
  (search-res-sel state last <))

(defn search-res-reset [state]
  (assoc-in state [:dir :search-res] []))

(defn update-search-results [{{:keys [files]} :dir :as state}
                             query]
  (if (not-empty query)
    (let [search-res (vec (for [i (range (count files))
                                :let [name (get-in files [i :name])]
                                :when (.contains name (apply str query))]
                            i))]
      (-> state
          (assoc-in [:dir :search-res] search-res)
          (search-res-down)))
    (assoc-in state [:dir :search-res] [])))

(defn toggle-split-preview-mode [{:keys [mode dir] :as state}]
  (case mode
    :split (assoc state :mode :prev)
    :prev (as-> state st
            (assoc st :mode :split)
            (if-not (st :split-dir)
              (assoc st :split-dir dir)
              st))
    :else state))

(defn split-mode-swap [{:keys [dir split-dir] :as state}]
  (-> state
      (assoc :dir split-dir)
      (assoc :split-dir dir)
      (update-after-sel-change)))
