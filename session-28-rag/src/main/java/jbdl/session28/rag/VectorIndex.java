package jbdl.session28.rag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * An in-memory vector store: every chunk with its vector, and a search that returns the chunks
 * whose vectors are most similar to the question's vector. A vector database (pgvector,
 * Elasticsearch, Qdrant, ...) does the same with millions of vectors, on disk, with clever indexes;
 * this one compares the question with every chunk, which is fine for a few thousand.
 */
public final class VectorIndex {

    /**
     * A found chunk and how similar it is to the question.
     *
     * @param chunk the chunk
     * @param score the cosine similarity, from 0 to 1
     */
    public record Hit(Chunk chunk, double score) {
    }

    private final List<Chunk> chunks;
    private final List<Map<String, Double>> vectors = new ArrayList<>();
    private final TextVectorizer vectorizer;

    private VectorIndex(List<Chunk> chunks, boolean stemming) {
        this.chunks = List.copyOf(chunks);
        List<String> texts = new ArrayList<>();
        for (Chunk chunk : chunks) {
            texts.add(indexedText(chunk));
        }
        this.vectorizer = TextVectorizer.fit(texts, stemming);
        for (String text : texts) {
            vectors.add(vectorizer.vector(text));
        }
    }

    /** Builds the index with stemming (topic 5 measures why): {@code of(chunks, true)}. */
    public static VectorIndex of(List<Chunk> chunks) {
        return of(chunks, true);
    }

    /** Builds the index: learns the word weights from all chunks, then stores one vector per chunk. */
    public static VectorIndex of(List<Chunk> chunks, boolean stemming) {
        return new VectorIndex(chunks, stemming);
    }

    /** The {@code k} chunks most similar to {@code question}, best first; chunks with score 0 are left out. */
    public List<Hit> search(String question, int k) {
        Map<String, Double> query = vectorizer.vector(question);
        List<Hit> hits = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            double score = TextVectorizer.cosine(query, vectors.get(i));
            if (score > 0) {
                hits.add(new Hit(chunks.get(i), score));
            }
        }
        // sort by score, highest first. The simpler version: a loop that picks the best k by hand.
        hits.sort((a, b) -> Double.compare(b.score(), a.score()));
        return hits.subList(0, Math.min(k, hits.size()));
    }

    /** How many chunks are in the index. */
    public int size() {
        return chunks.size();
    }

    /** The file path and the heading are searched too: "redis-caching" says a lot about a chunk. */
    private static String indexedText(Chunk chunk) {
        return chunk.source() + " " + chunk.heading() + " " + chunk.text();
    }
}
