(ns terminal-app.core
  (:require [lanterna.screen :as s])
  (:require [terminal-app.navigate :as nav])
  (:gen-class))

(if true
  (def scr (s/get-screen :text))
  (def scr (s/get-screen :swing)))

(defn prepare-line [files i width]
  (if (< i (count files))
    (let [line (files i)]
      (str " "
           (subs line 0 (min (count line) width))
           (apply str (repeat (- width (count line)) \space))))
    (apply str (repeat (inc width) \space))))

(defn rerender-sel [state]
  (s/put-string scr
                40
                (state :sel-old)
                (prepare-line (state :files) (state :sel-old) 40))
  (s/put-string scr
                40
                (state :sel)
                (prepare-line (state :files) (state :sel) 40)
                {:fg :black :bg :yellow}))

(defn render-files [col-start col-width row-start row-height files sel]
    (doseq [i (range row-height)]
      (s/put-string scr
                    col-start
                    (+ i row-start)
                    (prepare-line files i col-width)
                    (if (= i sel) {:fg :black
                                   :bg :cyan} {}))))

(defn do-render [state]
  (let [ly (state :layout)
        height (- (get-in ly [:size 1]) (ly :top-bar-height) (ly :bottom-bar-height))]
    (render-files 0
                  (- (ly :col1-char) 2)
                  (ly :top-bar-height)
                  height
                  (state :files)
                  (state :sel))
    (render-files (ly :col1-char)
                  (- (ly :col2-char) (ly :col1-char) 2)
                  (ly :top-bar-height)
                  height
                  (state :files)
                  (state :sel))
    (render-files (ly :col2-char)
                  (- (get-in ly [:size 0]) (ly :col2-char))
                  (ly :top-bar-height)
                  height
                  (state :files)
                  (state :sel))))

(defn handle-input [state]
  (case (s/get-key-blocking scr)
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
                 :list-height (- (new-size 1) (ly :top-bar-height))
                 :col1-char (int (* (new-size 0) (ly :col1-percent)))
                 :col2-char (int (* (new-size 0) (ly :col2-percent))))
      state)))

;; (pprint (get-screen-size {:file nil
;;                           :files nil
;;                           :sel 0
;;                           :sel-prev nil
;;                           :scroll-pos 0
;;                           :layout {:size []
;;                                    :top-bar-height 1
;;                                    :bottom-bar-height 1
;;                                    :col1-percent 0.2
;;                                    :col2-percent 0.6}}))

(defn input-cycle [state]
  (when state ((do-render (get-screen-size state))
               (s/redraw scr)
               (input-cycle (handle-input state)))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (s/start scr)
  (input-cycle (nav/init-state "/home/kai"))
  (s/stop scr))
