package jbdl.session18.github;

/**
 * One repository, as this app returns it: only the fields it needs, not GitHub's 100-field answer.
 *
 * A record is a short way to write a class that only holds data (Java 16+). The simpler, longer
 * version is a class with five private final fields, a constructor that sets them, and a getter for
 * each; the record writes those for us, plus equals, hashCode and toString.
 */
public record RepoSummary(String name, String description, String language, int stars, String url) {
}
