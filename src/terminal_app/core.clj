(ns terminal-app.core
  (:require [lanterna.screen :as s])
  (:require [terminal-app.navigate :as nav])
  (:require [clojure.java.io :as io])
  (:gen-class))

(if true
  (def scr (s/get-screen :text))
  (def scr (s/get-screen :swing)))

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
  (if (= (+ i (dir :scroll-pos)) (dir :sel)) {:fg :black
                                              :bg :cyan}
      (if (get-in dir [:files i :nodir?])
        {}
        {:fg :cyan})))

(defn render-files [col-start col-width ly dir]
  (doseq [i (range (ly :list-height))]
    (s/put-string scr
                  col-start
                  (+ i (ly :top-bar-height))
                  (prepare-line (mapv (fn [n] (n :name)) (dir :files))
                                (+ i (dir :scroll-pos))
                                col-width)
                  (get-line-style dir i))))

(defn render-prev-text [col-start col-width ly content]
  (doseq [i (range (ly :list-height))]
    (s/put-string scr
                  col-start
                  (+ i (ly :top-bar-height))
                  (prepare-line content i  col-width))))

(defn render-top-bar [width top-bar]
  (let [path-len (count (top-bar :path))]
    (s/put-string scr 0 0
                  (str " " (top-bar :path) "/")
                  {:fg :cyan})
    (s/put-string scr (+ path-len 2) 0
                  (str (top-bar :file)
                       (get-empty-str (- width
                                         path-len
                                         (count (top-bar :file))
                                         2))))))

(defn do-render [state]
  (let [ly (state :layout)]
    (render-top-bar (get-in ly [:size 0])
                    (state :top-bar))
    (render-files 0
                  (- (ly :col1-char) 2)
                  ly
                  (state :par-dir))
    (render-files (ly :col1-char)
                  (- (ly :col2-char) (ly :col1-char) 2)
                  ly
                  (state :dir))
    (let [col-start (ly :col2-char)
          col-width (- (get-in ly [:size 0]) (ly :col2-char))]
      (if-let [content (get-in state [:prev-dir :content])]
        (render-prev-text col-start
                          col-width
                          ly
                          content)
        (render-files col-start
                      col-width
                      ly
                      (state :prev-dir))))))

(defn exit [state]
  (s/stop scr)
  (println (str " exit-path: " (.getAbsolutePath (get-in state [:dir :file]))))
  (System/exit 1))

(defn render-search [state query]
  (let [size (get-in state [:layout :size])
        query-str (apply str query)]
    (s/put-string scr
                  0
                  (- (size 1) 1)
                  (apply str
                         " /"
                         query-str
                         (get-empty-str (- (size 0) (count query)))))
    (s/move-cursor scr
                   (+ 2 (count query))
                   (- (size 1) 1))))

(defn search-files [state query]
  (let [st (nav/update-search-results state query)]
    (render-search st query)
    (do-render st)
    (s/redraw scr)
    (let [key-res (s/get-key-blocking scr {:interval 5})]
      (case key-res
        :escape st
        :enter st
        :backspace (search-files st(if (not-empty query)
                                         (pop query)
                                         query))
        (search-files st(if (char? key-res)
                              (conj query key-res)
                              query))))))

(defn handle-input [state]
  (case (s/get-key-blocking scr {:interval 5})
    \j (nav/sel-down state)
    \k (nav/sel-up state)
    \h (nav/folder-up state)
    \l (nav/folder-down state)
    \/ (search-files state [])
    \n (nav/search-res-down state)
    \q (exit state)
    state))

(defn get-screen-size [state]
  (let [new-size (s/get-size scr)
        ly (state :layout)]
    (if-not (= new-size (get-in state [:layout :size]))
      (update-in state [:layout] assoc
                 :size new-size
                 :list-height (- (new-size 1) (ly :top-bar-height) (ly :bottom-bar-height))
                 :col1-char (int (* (new-size 0) (ly :col1-percent)))
                 :col2-char (int (* (new-size 0) (ly :col2-percent))))
      state)))

(defn input-cycle [state]
  (when state (let [st (get-screen-size state)]
                (do-render st)
                (s/redraw scr)
                (input-cycle (handle-input st)))))

(defn -main
  [& args]
  (let [path (if (and (> (count args) 0)
                      (.exists (io/file (first args))))
               (first args)
               "/home")]
    (s/start scr)
    (input-cycle (nav/init-state path))
    ;; (input-cycle (nav/init-state "/home/kai"))
    (s/stop scr)
    (System/exit 1)))
