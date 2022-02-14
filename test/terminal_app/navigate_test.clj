(ns terminal-app.navigate-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [terminal-app.navigate :refer :all]
            [test-with-files.tools :refer [with-tmp-dir]]))

;; (deftest navigate-test
;;   (testing "file-list sorting"
;;     (is (= [{}] 1))))
;;
(def tmp-dir "/")

(deftest get-preview
  (is (= (with-tmp-dir tmp-dir
           ;; (.mkdir (io/file tmp-dir "folder"))
           (spit (io/file tmp-dir "foo.txt") "I'm here")
           (get-preview! (generate-file-list (io/file tmp-dir))))
         [{:file nil, :filess nil, :sel nil, :scroll-pos nil, :content ["I'm here"]}])
      "gets a 'files'-vector and returns a vector with directory information or file information/content for every file in it"))
