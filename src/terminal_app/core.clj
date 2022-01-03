(ns terminal-app.core
  (:require [lanterna.screen :as s])
  (:require [terminal-app.navigate :as nav])
  (:gen-class))

(if true
  (def scr (s/get-screen :text))
  (def scr (s/get-screen :swing)))

(defn prepare-line [f-list i width]
  (if (< i (count f-list))
    (let [line (f-list i)]
      (str " "
           (subs line 0 (min (count line) width))
           (apply str (repeat (- width (count line)) \space))))
    (apply str (repeat (inc width) \space))))

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

(defn do-render [state]
  (let [ly (state :layout)]
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

(defn handle-input [state]
  (case (s/get-key-blocking scr {:interval 5})
    \j (nav/sel-down state)
    \k (nav/sel-up state)
    \h (nav/folder-up state)
    \l (nav/folder-down state)
    \q nil
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
  "I don't do a whole lot ... yet."
  [& args]
  (s/start scr)
  (input-cycle (nav/init-state "/home/kai"))
  (s/stop scr)
  (System/exit 1))
