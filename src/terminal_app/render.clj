(ns terminal-app.render
  (:require [lanterna.screen :as s]))

(defn get-empty-str [len]
  (apply str (repeat len \space)))

(defn prepare-line [f-list i width]
  (if (< i (count f-list))
    (let [line (f-list i)]
      (str " "
           (subs line 0 (min (count line) width))
           (get-empty-str (- width (count line)))))
    (get-empty-str (inc width))))

(defn get-line-style [dir i]
  (let [sel (= i (dir :sel))
        res (and (contains? dir :search-res) (.contains (dir :search-res) i))
        dir (not (get-in dir [:files i :nodir?]))]
    (cond sel {:fg :black :bg :cyan}
          res {:fg :yellow}
          dir {:fg :cyan}
          :else {})))

(defn render-files [put-string col-start col-width ly dir]
  (doseq [i (range (ly :list-height))]
    (let [current-file (+ i (dir :scroll-pos))]
      (put-string col-start
                  (+ i (ly :top-bar-height))
                  (prepare-line (mapv (fn [n] (n :name)) (dir :files))
                                current-file
                                col-width)
                  (get-line-style dir current-file)))))

(defn render-prev-text [put-string col-start col-width ly content]
  (doseq [i (range (ly :list-height))]
    (put-string col-start
                (+ i (ly :top-bar-height))
                (prepare-line content i  col-width))))

(defn render-top-bar [put-string width top-bar]
  (let [path-len (count (top-bar :path))]
    (put-string 0 0
                (str " " (top-bar :path) "/")
                {:fg :cyan})
    (put-string (+ path-len 2) 0
                (str (top-bar :file)
                     (get-empty-str (- width
                                       path-len
                                       (count (top-bar :file))
                                       2))))))

(defn render-search [state query]
  (let [size (get-in state [:layout :size])
        bottom-line (- (size 1) 1)
        query-str (apply str query)
        res-count (count (get-in state [:dir :search-res]))
        count-str (str "number of results: " res-count)
        scr (state :scr)]
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

(defn do-render [state]
  (let [ly (state :layout)
        put-string (partial s/put-string (state :scr))]
    (render-top-bar put-string
                    (get-in ly [:size 0])
                    (state :top-bar))
    (render-files put-string
                  0
                  (- (ly :col1-char) 2)
                  ly
                  (state :par-dir))
    (render-files put-string
                  (ly :col1-char)
                  (- (ly :col2-char) (ly :col1-char) 2)
                  ly
                  (state :dir))
    (let [col-start (ly :col2-char)
          col-width (- (get-in ly [:size 0]) (ly :col2-char))]
      (if-let [content (get-in state [:prev-dir :content])]
        (render-prev-text put-string
                          col-start
                          col-width
                          ly
                          content)
        (render-files put-string
                      col-start
                      col-width
                      ly
                      (state :prev-dir))))))
