package jbdl.session18.github;

/** GitHub refused or failed a request; the message says what to do next. */
public class GitHubApiException extends RuntimeException {

    // Exceptions are Serializable; the build treats the missing version number as an error (Session 2).
    private static final long serialVersionUID = 1L;

    private final int gitHubStatus;

    public GitHubApiException(int gitHubStatus, String message) {
        super(message);
        this.gitHubStatus = gitHubStatus;
    }

    /** The HTTP status GitHub answered with (401, 403, 500 ...). */
    public int getGitHubStatus() {
        return gitHubStatus;
    }
}
