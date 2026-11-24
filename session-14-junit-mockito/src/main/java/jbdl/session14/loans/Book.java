package jbdl.session14.loans;

/**
 * A book title and how many copies are on the shelf right now.
 *
 * One Book object is shared by every request (InMemoryLibraryRepository keeps it in a map), so two
 * borrowers can call takeCopy() at the same moment. "Check, then decrement" is two steps: without a lock
 * both see 1 copy left and both take it, and the count goes to -1. `synchronized` makes each method run
 * alone on this object (Session 5, topic 3). With a database the same rule is one atomic statement:
 * UPDATE book SET copies = copies - 1 WHERE id = ? AND copies > 0, then check that one row changed.
 */
public class Book {

    private final long id;
    private final String title;
    private int copiesAvailable;

    public Book(long id, String title, int copiesAvailable) {
        this.id = id;
        this.title = title;
        this.copiesAvailable = copiesAvailable;
    }

    /** Takes one copy off the shelf; refuses when none is left. */
    public synchronized void takeCopy() {
        if (copiesAvailable == 0) {
            throw new IllegalStateException("no copies of \"" + title + "\" left");
        }
        copiesAvailable--;
    }

    public synchronized void returnCopy() {
        copiesAvailable++;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public synchronized int getCopiesAvailable() {
        return copiesAvailable;
    }
}
