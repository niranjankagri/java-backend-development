# Topic 2: Embeddings, vector databases and semantic search

To find "the chunks that match a question", text must become something a computer can compare:
a **vector**, a list of numbers. Similar texts get similar vectors; the search returns the chunks
whose vectors are closest to the question's.

## Vectors from words: TF-IDF (what this session builds)

[`TextVectorizer`](../../src/main/java/jbdl/session28/rag/TextVectorizer.java) uses only the JDK:

- one dimension per word; a chunk's vector holds a number for each word in it;
- **TF** (term frequency): a word that appears more often counts more;
- **IDF** (inverse document frequency): a rare word counts more. "redis" says a lot about a chunk,
  "the" says nothing (and is dropped as a stop word);
- the vector is scaled to length 1, so long chunks do not win just by being long.

```java
// log((1 + N) / (1 + count)) + 1: rare words get a high weight, common words about 1
idf.put(entry.getKey(), Math.log((1.0 + texts.size()) / (1.0 + entry.getValue())) + 1.0);
```

Comparing two vectors is the **cosine similarity**: for vectors of length 1, the sum of the products
of the words they share. 0 = no word in common, 1 = the same direction.

```java
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
```

## Vectors from meaning: embeddings

An **embedding model** is a neural network that turns a text into a vector of a few hundred to a few
thousand numbers, trained so that texts with the **same meaning** end up close together, even with
no word in common. Providers offer them through an API (OpenAI, Google, Cohere, Voyage AI, which
Anthropic recommends for Claude; Anthropic itself has no embedding endpoint), and open models run
locally.

The difference, as a test in
[`TextVectorizerTest`](../../src/test/java/jbdl/session28/rag/TextVectorizerTest.java):

```java
@Test
void sameMeaningButNoCommonWordGivesZero() {
    // the limit of word-based vectors: a person sees "money back" = "refund", TF-IDF sees no overlap.
    // An embedding model (topic 2) puts these two close together.
    ...
    assertThat(similarity).isZero();
}
```

| | TF-IDF (words) | Embeddings (meaning) |
|---|---|---|
| "money back" vs "refund" | 0, no common word | close |
| exact names: `PESSIMISTIC_WRITE`, `8200`, an error code | strong | can be weaker |
| needs | the JDK | a model (API costs or a local model) |
| explainable | yes: these words matched | hardly: 1,024 numbers |

That is why real systems often use both (**hybrid search**): words for exact names, embeddings for
meaning.

## Semantic search and vector databases

**Semantic search** = searching by meaning, with embeddings. Storing and searching millions of
vectors needs a **vector database**: it keeps the vectors on disk and uses special indexes (such as
HNSW) to find the nearest ones without comparing the question with every vector. Examples:
**pgvector** (an extension for PostgreSQL, which the Airbnb project already uses), Elasticsearch and
OpenSearch, Qdrant, Weaviate, Pinecone.

[`VectorIndex`](../../src/main/java/jbdl/session28/rag/VectorIndex.java) is the smallest possible
version: a list in memory, compared one by one. For the 833 chunks of this course it searches in
milliseconds; for millions it would not.

```java
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
```
