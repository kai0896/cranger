(ns terminal-app.core
  (:require [lanterna.screen :as s])
  (:require [terminal-app.navigate :as nav])
  (:require [terminal-app.render :as rdr])
  (:require [clojure.java.io :as io])
  (:gen-class))

(defn exit [state]
  (s/stop (state :scr))
  (println (str " exit-path: " (.getAbsolutePath (get-in state [:dir :file]))))
  (System/exit 1))

(defn search-files [state query]
  (let [st (nav/update-search-results state query)
        scr (st :scr)]
    (rdr/do-render st)
    (rdr/render-search st query)
    (s/redraw scr)
    (let [key-res (s/get-key-blocking scr {:interval 5})]
      (case key-res
        :escape (nav/search-res-reset st)
        :enter st
        :backspace (search-files st(if (not-empty query)
                                         (pop query)
                                         query))
        (search-files st(if (char? key-res)
                              (conj query key-res)
                              query))))))

(defn handle-input [state]
  (case (s/get-key-blocking (state :scr) {:interval 5})
    \j (nav/sel-down state)
    \k (nav/sel-up state)
    \h (nav/folder-up state)
    \l (nav/folder-down state)
    \g (nav/sel-top state)
    \G (nav/sel-bottom state)
    \/ (search-files state [])
    \n (nav/search-res-down state)
    \N (nav/search-res-up state)
    :escape (nav/search-res-reset state)
    \q (exit state)
    state))

(defn get-screen-size [state]
  (let [new-size (s/get-size (state :scr))
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
                (rdr/do-render st)
                (s/redraw (st :scr))
                (input-cycle (handle-input st)))))

(if-not true
  (def scr (s/get-screen :swing))
  ())

(defn -main
  [& args]
  (let [path (if (and (> (count args) 0)
                      (.exists (io/file (first args))))
               (first args)
               "/home")
        scr (s/get-screen :text)]
    (s/start scr)
    (input-cycle (nav/init-state path scr))
    ;; (input-cycle (nav/init-state "/home/kai") scr)
    (s/stop scr)
    (System/exit 1)))
