(ns terminal-app.render
  (:require [lanterna.screen :as s]))

(defn get-empty-str [len]
  (apply str (repeat len \space)))

(defn prepare-line [files i width details?]
  (if (< i (count files))
    (let [name (get-in files [i :name])
          file-size (if details? (get-in files [i :size]) "")
          space-for-name (- width (count file-size) 1)]
      (str " "
           (if (> (count name) (- space-for-name 1))
             (str (subs name 0 (- space-for-name 2)) "… ")
             name)
           (get-empty-str (- space-for-name (count name)))
           file-size
           " "))
    (get-empty-str (inc width))))

(defn get-line-style [dir i colors]
  (let [sel? (= i (dir :sel))
        res? (and (contains? dir :search-res) (.contains (dir :search-res) i))
        dir? (not (get-in dir [:files i :nodir?]))
        color (cond res? (colors :search-res)
                    dir? (colors :primary)
                    :else (colors :text))]
    (if sel?
      {:fg :black :bg (if (= color :default) :white color)}
      {:fg color})))

(defn render-files [put-string col-start col-width ly dir details?]
  (doseq [i (range (ly :list-height))]
    (let [current-file (+ i (dir :scroll-pos))
          pos-y (+ i (ly :top-bar-height))]
      (put-string col-start pos-y " ")
      (put-string (+ col-start 1)
                  pos-y
                  (prepare-line (dir :files)
                                current-file
                                col-width
                                details?)
                  (get-line-style dir current-file (ly :colors))))))

(defn render-prev-text [put-string col-start col-width ly content]
  (doseq [i (range (ly :list-height))]
    (put-string col-start
                (+ i (ly :top-bar-height))
                (if (< i (count content))
                  (let [line (content i)]
                    (str " "
                         line
                         (get-empty-str (- col-width (count line)))))
                  (get-empty-str (inc col-width))))))

(defn render-top-bar [put-string width top-bar mode colors]
  (let [path-len (count (top-bar :path))]
    (put-string 0 0
                (str " " (top-bar :path) "/")
                {:fg (colors :primary)})
    (put-string (+ path-len 2) 0
                (str (top-bar :file)
                     (get-empty-str (- width
                                       path-len
                                       (count (top-bar :file))
                                       2))))))

(defn render-bottom-bar [put-string ly dir mode]
  (let [selection-display (str (+ 1 (dir :sel))
                               "/"
                               (count (dir :files)))
        mode-info (case mode
                    :prev "Preview-Mode"
                    :split "Split-Mode"
                    :else "")
        text-right (str mode-info " " selection-display)
        size (ly :size)
        bottom-line (- (size 1) 1)]
    (put-string 0
                bottom-line
                (get-empty-str (size 0)))
    (put-string (- (size 0) (count text-right))
                bottom-line
                text-right)))

(defn render-search [state scr query]
  (let [size (get-in state [:layout :size])
        bottom-line (- (size 1) 1)
        query-str (apply str query)
        res-count (count (get-in state [:dir :search-res]))
        count-str (str "number of results: " res-count)]
    (s/put-string scr 0 bottom-line
                  (str " /"
                       query-str
                       (get-empty-str (- (size 0) (count query)))))
    (s/put-string scr (- (size 0) (count count-str)) bottom-line
                  count-str
                  (if (> res-count 0)
                    {:fg :yellow}
                    {:fg :red}))
    (s/move-cursor scr
                   (+ 2 (count query))
                   (- (size 1) 1))))

(defn do-render [state scr]
  (let [ly (state :layout)
        put-string (partial s/put-string scr)]
    (s/move-cursor scr 0 (- (get-in state [:layout :size 1]) 1))
    (render-top-bar put-string
                    (get-in ly [:size 0])
                    (state :top-bar)
                    (state :mode)
                    (get-in state [:layout :colors]))
    (render-bottom-bar put-string
                       ly
                       (state :dir)
                       (state :mode))
    (render-files put-string
                  -1
                  (- (ly :col1-char) 1)
                  ly
                  (state :par-dir)
                  false)
    (render-files put-string
                  (ly :col1-char)
                  (- (ly :col2-char) (ly :col1-char) 2)
                  ly
                  (state :dir)
                  true)
    (let [col-start (ly :col2-char)
          col-width (- (get-in ly [:size 0]) (ly :col2-char))]
      (case (state :mode)
        :split (render-files put-string
                             col-start
                             col-width
                             (assoc-in ly [:colors :primary] (get-in ly [:colors :split-mode]))
                             (state :split-dir)
                             false)
        :prev (let [sel (get-in state [:dir :sel])
                    prev-dir (get-in state [:preview sel])]
                (if-let [content (prev-dir :content)]
                  (render-prev-text put-string
                                    col-start
                                    col-width
                                    ly
                                    content)
                  (render-files put-string
                                col-start
                                col-width
                                ly
                                prev-dir
                                false)))))))
