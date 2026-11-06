package jbdl.session14.loans;

/** A book title and how many copies are on the shelf right now. */
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
    public void takeCopy() {
        if (copiesAvailable == 0) {
            throw new IllegalStateException("no copies of \"" + title + "\" left");
        }
        copiesAvailable--;
    }

    public void returnCopy() {
        copiesAvailable++;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getCopiesAvailable() {
        return copiesAvailable;
    }
}
