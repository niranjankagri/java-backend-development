package jbdl.session10.students;

/**
 * One row of the student table as a Java object. id is a Long (not long) so a new student that has
 * no id yet can carry null.
 */
public record Student(Long id, String name, String email, int marks, String courseCode) {

    /** A student that isn't saved yet: the database will assign the id. */
    public static Student newStudent(String name, String email, int marks, String courseCode) {
        return new Student(null, name, email, marks, courseCode);
    }

    public Student withId(long newId) {
        return new Student(newId, name, email, marks, courseCode);
    }
}
