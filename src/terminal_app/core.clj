(ns terminal-app.core
  (:require [terminal-app.navigate :as nav])
  (:require [terminal-app.render :as rdr])
  (:require [terminal-app.config :refer [default-config]])
  (:require [clojure.java.io :as io])
  (:require [clojure.edn :as edn])
  (:require [lanterna.screen :as s])
  (:gen-class))

(defn get-config!
  "check for config file in home dir and merge it with default config if found"
  []
  ;; TODO check that custom config is not breaking the program
  (if-let [custome-config (let [home (System/getProperty "user.home")
                                conf-path (str home "/.config/cranger/config.edn")
                                home-path (str home "/.cranger/config.edn")
                                in-conf-dir? (.exists (io/file conf-path))
                                in-home-dir? (.exists (io/file home-path))]
                            (cond in-conf-dir? (edn/read-string (slurp conf-path))
                                  in-home-dir? (edn/read-string (slurp home-path))))]
    (merge-with merge default-config custome-config)
    default-config))

(defn exit [state scr]
  (s/stop scr)
  (println (str " \n exit-path: " (get-in state [:dir :path])))
  (System/exit 1))

(defn search-files
  "recursively ask for next char for the search query and catch exit conditions that return the new state"
  [state scr query]
  (let [st (nav/update-search-results state query)]
    (rdr/do-render st scr)
    (rdr/render-search st scr query)
    (s/redraw scr)
    (let [key-res (s/get-key-blocking scr {:interval 5})]
      (case key-res
        :escape (nav/search-res-reset st)
        :enter st
        :backspace (search-files st scr (if (not-empty query)
                                          (pop query)
                                          query))
        (search-files st scr (if (char? key-res)
                               (conj query key-res)
                               query))))))

(defn handle-input!
  "wait for input and call appropriate functions to change the state"
  [state scr]
  (let [key-char (s/get-key-blocking scr {:interval 5})
        key-keyword (get-in state [:keybinds key-char] nil)]
    (case key-keyword
      :sel-down (nav/sel-down state)
      :sel-up (nav/sel-up state)
      :folder-up (nav/folder-up! state)
      :folder-down (nav/folder-down! state)
      :sel-top (nav/sel-top state)
      :sel-bottom (nav/sel-bottom state)
      :search-files (search-files state scr [])
      :search-result-down (nav/search-res-down state)
      :search-result-up (nav/search-res-up state)
      :search-result-reset (nav/search-res-reset state)
      :toggle-mode (nav/toggle-split-preview-mode! state)
      :split-mode-swap (nav/split-mode-swap state)
      :copy (nav/copy-sel! state)
      :exit (exit state scr)
      state)))

(defn check-window-size!
  "check if window size changed and adjust state accordingly"
  [state scr]
  (let [new-size (s/get-size scr)]
    (if-not (= new-size (get-in state [:layout :size]))
      (nav/resize-screen state new-size)
      state)))

(defn input-cycle
  "recursivly render, wait for input and adjust state accordingly"
  [state scr]
  (when state (let [st (check-window-size! state scr)]
                (rdr/do-render st scr)
                (s/redraw scr)
                (input-cycle (handle-input! st scr) scr))))

(if-not true
  (def scr (s/get-screen :swing))
  ())

(defn -main
  "start screen, initialize state and main loop"
  [& args]
  (let [path (if (and (> (count args) 0)
                      (.exists (io/file (first args))))
               (first args)
               (System/getProperty "user.home"))
        scr (s/get-screen :text)]
    (s/start scr)
    (input-cycle (nav/init-state! path (get-config!)) scr)
    (s/stop scr)
    (System/exit 1)))
