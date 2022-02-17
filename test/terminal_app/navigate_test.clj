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

(deftest get-preview-test
  (is (= (with-tmp-dir tmp-dir
           ;; (.mkdir (io/file tmp-dir "folder"))
           (spit (io/file tmp-dir "foo.txt") "I'm here")
           (get-preview! (generate-file-list! tmp-dir)))
         [{:path nil, :filess nil, :sel nil, :scroll-pos nil, :content ["I'm here"]}])
      "gets a 'files'-vector and returns a vector with directory information or file information/content for every file in it"))

(deftest sort-files-test
  (is (= (sort-files [{:name "b" :nodir? nil :hidden? nil}
                      {:name "a" :nodir? true :hidden? nil}
                      {:name "c" :nodir? true :hidden? true}
                      {:name "a" :nodir? nil :hidden? true}])
         [{:name "b" :nodir? nil :hidden? nil}
          {:name "a" :nodir? nil :hidden? true}
          {:name "a" :nodir? true :hidden? nil}
          {:name "c" :nodir? true :hidden? true}])))

(deftest bytes-to-n-test
  (is (= (bytes-to-n 10000 1)
         "9.765"))
  (is (= (bytes-to-n 1000000 2)
         "0.953")))

(deftest get-file-size-str-test
  (is (= (with-tmp-dir tmp-dir
           (spit (io/file tmp-dir "foo.txt") "I'm here")
           (get-file-size-str (io/file tmp-dir "foo.txt")))
         "8 B")))
