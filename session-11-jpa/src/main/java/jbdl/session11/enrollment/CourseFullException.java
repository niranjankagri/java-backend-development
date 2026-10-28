package jbdl.session11.enrollment;

/** Unchecked (a RuntimeException): by default @Transactional ROLLS BACK when it is thrown. */
public class CourseFullException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CourseFullException(String courseTitle) {
        super("course " + courseTitle + " is full");
    }
}
