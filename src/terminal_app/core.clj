(ns terminal-app.core
  (:require [terminal-app.navigate :as nav])
  (:require [terminal-app.render :as rdr])
  (:require [terminal-app.config :refer [default-config]])
  (:require [clojure.java.io :as io])
  (:require [clojure.edn :as edn])
  (:require [lanterna.screen :as s])
  (:gen-class))

(def custome-config
  (let [home (System/getProperty "user.home")
        conf-path (str home "/.config/cranger/config.edn")
        home-path (str home "/.cranger/config.edn")
        in-conf-dir? (.exists (io/file conf-path))
        in-home-dir? (.exists (io/file home-path))]
    (cond in-conf-dir? (edn/read-string (slurp conf-path))
          in-home-dir? (edn/read-string (slurp home-path)))))

(def config
  (if custome-config
    (merge-with merge default-config custome-config)
    default-config))

config

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
        :backspace (search-files st (if (not-empty query)
                                      (pop query)
                                      query))
        (search-files st (if (char? key-res)
                           (conj query key-res)
                           query))))))

(defn handle-input [state]
  (let [key-char (s/get-key-blocking (state :scr) {:interval 5})
        key-keyword (get-in config [:keybinds key-char] nil)]
    (case key-keyword
      :sel-down (nav/sel-down state)
      :sel-up (nav/sel-up state)
      :folder-up (nav/folder-up state)
      :folder-down (nav/folder-down state)
      :sel-top (nav/sel-top state)
      :sel-bottom (nav/sel-bottom state)
      :search-files (search-files state [])
      :search-result-down (nav/search-res-down state)
      :search-result-up (nav/search-res-up state)
      :search-result-reset (nav/search-res-reset state)
      :toggle-mode (nav/toggle-split-preview-mode state)
      :split-mode-swap (nav/split-mode-swap state)
      :exit (exit state)
      state)))

(defn check-window-size [state]
  (let [new-size (s/get-size (state :scr))]
    (if-not (= new-size (get-in state [:layout :size]))
      (nav/resize-screen state new-size)
      state)))

(defn input-cycle [state]
  (when state (let [st (check-window-size state)]
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
               (System/getProperty "user.home"))
        scr (s/get-screen :text)]
    (s/start scr)
    (input-cycle (nav/init-state! path scr (config :colors)))
    ;; (input-cycle (nav/init-state "/home/kai") scr)
    (s/stop scr)
    (System/exit 1)))
