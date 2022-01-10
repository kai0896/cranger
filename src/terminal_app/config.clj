(ns terminal-app.config)

(def keybinds {
   \j       :sel-down
   \k       :sel-up
   \h       :folder-up
   \l       :folder-down
   :down    :sel-down
   :up      :sel-up
   :left    :folder-up
   :right   :folder-down
   \g       :sel-top
   \G       :sel-bottom
   \/       :search-files
   \n       :search-result-down
   \N       :search-result-up
   :escape  :search-result-reset
   \q       :exit
   })
