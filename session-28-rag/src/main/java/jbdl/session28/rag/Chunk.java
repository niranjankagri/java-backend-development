package jbdl.session28.rag;

/**
 * One piece of a document: the unit that is indexed, found and given to the model.
 *
 * @param source  where it comes from, e.g. session-15-redis-caching/README.md
 * @param heading the heading it belongs to, e.g. "What the real Redis taught us"
 * @param text    the text under that heading
 */
public record Chunk(String source, String heading, String text) {

    /** "source > heading", the way the chunk is cited in an answer. */
    public String reference() {
        return heading.isEmpty() ? source : source + " > " + heading;
    }
}
