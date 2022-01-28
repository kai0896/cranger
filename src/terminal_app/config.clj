(ns terminal-app.config)

(def default-config
  {:keybinds {\j :sel-down
              \k :sel-up
              \h :folder-up
              \l :folder-down
              :down :sel-down
              :up :sel-up
              :left :folder-up
              :right :folder-down
              \g :sel-top
              \G :sel-bottom
              \/ :search-files
              \n :search-result-down
              \N :search-result-up
              :escape :search-result-reset
              \m :toggle-mode
              \s :split-mode-swap
              \q :exit}
   :colors {:text :default
            :primary :cyan
            :split-mode :blue
            :search-res :yellow
            :visual-sel :green}})
