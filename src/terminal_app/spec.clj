(ns terminal-app.spec
  (:require [clojure.spec.alpha :as s])
  (:require [clojure.spec.test.alpha :as stest]))

(s/def :dir/path string?)
(s/def :dir/sel int?)
(s/def :dir/scroll-pos int?)

(s/def :file/name string?)
(s/def :file/size string?)
(s/def :file/nodir? boolean?)
(s/def :file/hidden? boolean?)

(s/def :dir/files (s/coll-of
                   (s/keys :req-un [:dir/path
                                    :file/name
                                    :file/size
                                    :file/nodir?
                                    :file/hidden?])))

(s/def :state/dir (s/keys :req-un [:dir/path
                                   :dir/files
                                   :dir/sel
                                   :dir/scroll-pos]))

(s/conform :state/dir {:path "sdf/sdf"
                       :files  [{:path "sdf"
                                 :name "sdf"
                                 :size "2"
                                 :nodir? false
                                 :hidden? true}]
                       :sel 9
                       :scroll-pos 3})

(defn change-sel [state]
  (assoc state :sel ""))

(s/fdef change-sel
  :args (s/cat :state :state/dir)
  :ret :state/dir)

(change-sel {:path "sdf/sdf"
                       :files  [{:path "sdf"
                                 :name "sdf"
                                 :size "2"
                                 :nodir? false
                                 :hidden? true}]
                       :sel 0
                       :scroll-pos 3})

(stest/instrument `changed-sel)
(stest/check `changed-sel)
