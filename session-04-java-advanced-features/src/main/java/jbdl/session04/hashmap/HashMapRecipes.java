package jbdl.session04.hashmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * The HashMap methods that replace the usual "if containsKey ... else put" boilerplate, and an LRU
 * cache built on LinkedHashMap.
 */
public final class HashMapRecipes {

    private HashMapRecipes() {
    }

    /** merge: insert 1 for a new word, otherwise add 1 to the old count. */
    public static Map<String, Integer> wordCounts(String text) {
        Map<String, Integer> counts = new TreeMap<>(); // sorted only so the output is readable
        for (String word : text.toLowerCase(Locale.ROOT).split("\\W+")) {
            if (!word.isEmpty()) {
                // merge = "put 1 if the word is new, otherwise old + 1". Without merge:
                //   Integer old = counts.get(word);
                //   counts.put(word, old == null ? 1 : old + 1);
                counts.merge(word, 1, Integer::sum);
            }
        }
        return counts;
    }

    /** computeIfAbsent: create the list the first time a key is seen, then add to it. */
    public static Map<Character, List<String>> byFirstLetter(List<String> words) {
        Map<Character, List<String>> groups = new TreeMap<>();
        for (String word : words) {
            // Without computeIfAbsent:
            //   List<String> list = groups.get(word.charAt(0));
            //   if (list == null) { list = new ArrayList<>(); groups.put(word.charAt(0), list); }
            //   list.add(word);
            groups.computeIfAbsent(word.charAt(0), letter -> new ArrayList<>()).add(word);
        }
        return groups;
    }

    /** getOrDefault: a value even for a missing key, without inserting it. */
    public static int stockOf(Map<String, Integer> stock, String product) {
        return stock.getOrDefault(product, 0);
    }

    /** putIfAbsent: keep the existing value; returns it (or null if the key was new). */
    public static String registerOnce(Map<String, String> owners, String username, String owner) {
        String existing = owners.putIfAbsent(username, owner);
        return existing == null ? owner : existing;
    }

    /** null is allowed once as a key and as values in HashMap (not in TreeMap, Map.of or ConcurrentHashMap). */
    public static Map<String, String> withNullKey() {
        Map<String, String> map = new HashMap<>();
        map.put(null, "no key");
        map.put("k", null);
        return map;
    }

    /**
     * LRU (least recently used) cache: LinkedHashMap in ACCESS order moves an entry to the end on
     * every get; removeEldestEntry drops the least recently used one once the cache is full.
     */
    public static <K, V> Map<K, V> lruCache(int capacity) {
        return new LinkedHashMap<>(16, 0.75f, true) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > capacity;
            }
        };
    }
}
