(ns donald-tweets.core
  (:require
      [clojure.string :as string]
      [clojure.set    :as set]
      [clojure.java.io :as io]
      [twitter.api.restful :as twitter]
      [twitter.oauth :as twitter-oauth]
      [twitter.callbacks :as callbacks]
      [twitter.callbacks.handlers :as callbacks-handlers]
      [environ.core :refer [env]]))


(def my-creds (twitter-oauth/make-oauth-creds (env :app-consumer-key)
                                              (env :app-consumer-secret)
                                              (env :user-access-token)
                                              (env :user-access-secret)))


; (def prefix-list ["Tremendous" "I am" "Thank you" "Great" "I will"])
(def prefix-list ["On the" "Thank you" "And all" "We think"
                  "For every" "No other" "To a" "And every"
                  "What A" "Watch" "And the" "But the"
                  "Are the" "Haters can" "For the" "When we"
                  "In the" "Yet we" "Thank you" "Are the"
                  "Make America"  "And when"
                  "We sit" "And this" "No other" "With a"
                  "And at" "What a" "Of the"
                  "ISIS has" "The great" "Such bigly" "When they"
                  "But before" "Tremendous" "And nobody" "And it's"
                  "For any" "For example," "I am" "I'm such"])
(defn word-chain [word-transitions]
  (reduce (fn [r t] (merge-with set/union r
                               (let [[a b c] t]
                                 {[a b] (if c #{c} #{})})))
          {}
          word-transitions))

(defn text->word-chain [string]
  (let [words (string/split string #"[\s|\n]")
        word-transitions (partition-all 3 1 words)]
    (word-chain word-transitions)))

(defn chain->text [chain]
  (apply str (interpose " " chain)))

(defn walk-chain [prefix chain result]
  (let [suffixes (get chain prefix)]
    ; (println result chain)
    (if
      (and
       (empty? suffixes)
       (> 50 (count result)))
      result
      (let [suffix (first (shuffle suffixes))
            new-prefix [(last prefix) suffix]
            result-with-spaces (chain->text result)
            result-char-count (count result-with-spaces)
            suffix-char-count (inc (count suffix))
            new-result-char-count (+ result-char-count suffix-char-count)]
        (if (>= new-result-char-count 140)
          result
          (recur new-prefix chain (conj result suffix)))))))

(defn end-at-last-punctuation [text]
  (let [trimmed-to-last-punct (apply str (re-seq #"[\s\w]+[^.!?,]*[.!?,]" text))
        trimmed-to-last-word (apply str (re-seq #".*[^a-zA-Z]+" text))
        result-text (if (empty? trimmed-to-last-punct)
                      trimmed-to-last-word
                      trimmed-to-last-punct)
        cleaned-text (clojure.string/replace result-text #"[,| ]$" ".")]
    (clojure.string/replace cleaned-text #"\"" "'")))

(defn generate-text
  [start-phrase word-chain]
  (let [prefix (string/split start-phrase #" ")
        result-chain (walk-chain prefix word-chain prefix)
        result-text (chain->text result-chain)]
    result-text))

(defn process-file [fname]
  (text->word-chain
   (slurp (io/resource fname))))

(defn tweet-text [text-source]
  (let [text (generate-text (-> prefix-list shuffle first) text-source)]
    (end-at-last-punctuation text)))

(defn status-update []
  (let [trump-tweets   (twitter/statuses-user-timeline :oauth-creds my-creds
                                    :params {:screen-name "realDonaldTrump"
                                             :count       200
                                             :includ-rts  1})
        text-unwrapped            (for [x (range (count (get trump-tweets :body)))]
                                      (get-in trump-tweets [:body x :text]))
        tweet          (tweet-text (text->word-chain (apply str text-unwrapped)))]
    (println "generated tweet is :" tweet)
    (println "char count is:" (count tweet))

    (if (> (count tweet) 50)
      (try (twitter/statuses-update :oauth-creds my-creds
                                    :params {:status tweet})
           (catch Exception e (println "Oh no! " (.getMessage e))))
      (status-update))))

(defn -main []
    (status-update))
