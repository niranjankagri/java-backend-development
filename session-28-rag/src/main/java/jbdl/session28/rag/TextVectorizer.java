package jbdl.session28.rag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Turns text into a vector of numbers, so that texts can be compared by arithmetic. This is
 * TF-IDF, the classic way, using only the JDK:
 *
 * <ul>
 *   <li>one dimension per word (the vector is a map from word to number);</li>
 *   <li>TF, term frequency: a word that appears often in the text counts more;</li>
 *   <li>IDF, inverse document frequency: a word that appears in few chunks counts more ("redis"
 *       says more about a chunk than "the", which is in all of them).</li>
 * </ul>
 *
 * <p>Real embedding models (topic 2) make vectors from the <b>meaning</b>, so "money back" lands near
 * "refund". TF-IDF only sees the <b>words</b>: "money back" and "refund" share no word, so their
 * vectors have nothing in common. The tests and topic 5 show this limit on purpose.
 */
public final class TextVectorizer {

    /** Very common English words that carry no meaning for search. */
    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "by", "can", "do", "does", "for", "from", "how",
            "if", "in", "is", "it", "its", "of", "on", "or", "that", "the", "this", "to", "was", "what",
            "when", "which", "who", "why", "with", "you", "your", "i", "we", "my", "our", "not", "no");

    /** For each word: how rare it is among all chunks. */
    private final Map<String, Double> idf;

    /** Whether words are cut to their stem ("failed" -> "fail"), see {@link #stem(String)}. */
    private final boolean stemming;

    private TextVectorizer(Map<String, Double> idf, boolean stemming) {
        this.idf = idf;
        this.stemming = stemming;
    }

    /** Learns the IDF of every word from all texts that will be searched. */
    public static TextVectorizer fit(List<String> texts, boolean stemming) {
        // in how many texts does each word appear?
        Map<String, Integer> documentCount = new HashMap<>();
        for (String text : texts) {
            for (String word : Set.copyOf(words(text, stemming))) { // Set.copyOf: count each word once per text
                documentCount.put(word, documentCount.getOrDefault(word, 0) + 1);
            }
        }
        Map<String, Double> idf = new HashMap<>();
        for (Map.Entry<String, Integer> entry : documentCount.entrySet()) {
            // log((1 + N) / (1 + count)) + 1: rare words get a high weight, common words about 1
            idf.put(entry.getKey(), Math.log((1.0 + texts.size()) / (1.0 + entry.getValue())) + 1.0);
        }
        return new TextVectorizer(idf, stemming);
    }

    /** The TF-IDF vector of {@code text}, scaled to length 1. Words never seen in fit() are ignored. */
    public Map<String, Double> vector(String text) {
        Map<String, Double> vector = new HashMap<>();
        for (String word : words(text, stemming)) {
            Double weight = idf.get(word);
            if (weight != null) {
                vector.put(word, vector.getOrDefault(word, 0.0) + weight); // TF x IDF, one word at a time
            }
        }
        // scale to length 1, so a long chunk does not win only because it is long
        double length = 0;
        for (double value : vector.values()) {
            length += value * value;
        }
        length = Math.sqrt(length);
        if (length > 0) {
            for (Map.Entry<String, Double> entry : vector.entrySet()) {
                entry.setValue(entry.getValue() / length);
            }
        }
        return vector;
    }

    /**
     * Cosine similarity of two vectors of length 1: from 0 (no word in common) to 1 (the same
     * direction). For vectors of length 1 it is just the sum of the products of matching words.
     */
    public static double cosine(Map<String, Double> a, Map<String, Double> b) {
        double sum = 0;
        for (Map.Entry<String, Double> entry : a.entrySet()) {
            Double other = b.get(entry.getKey());
            if (other != null) {
                sum += entry.getValue() * other;
            }
        }
        return sum;
    }

    /** The words of a text: lowercase letters and digits, without stop words and single letters. */
    static List<String> words(String text, boolean stemming) {
        List<String> words = new ArrayList<>();
        for (String word : text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+")) {
            if (word.length() > 1 && !STOP_WORDS.contains(word)) {
                words.add(stemming ? stem(word) : word);
            }
        }
        return words;
    }

    /**
     * A very small stemmer: cuts common English endings, so that "fail", "fails", "failed" and
     * "failing" become the same word. Real stemmers (Porter, Snowball) know many more rules; this one
     * shows the idea and is wrong now and then ("nights" -> "night" is right, "class" -> "clas" is
     * harmless, because the same cut happens in the question and in the chunks).
     */
    static String stem(String word) {
        String[] endings = {"ing", "ed", "es", "s"};
        for (String ending : endings) {
            // keep at least 3 letters, so "is" or "bed" stay as they are
            if (word.endsWith(ending) && word.length() - ending.length() >= 3) {
                return word.substring(0, word.length() - ending.length());
            }
        }
        return word;
    }
}
