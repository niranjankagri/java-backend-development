package jbdl.session11.school;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import java.util.HashSet;
import java.util.Set;

/** A student with a profile (one-to-one) and the courses they take (many-to-many). */
@Entity
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    /**
     * One student, one profile. cascade = ALL: saving or deleting the student does the same to the
     * profile, so we never save a profile on its own. orphanRemoval: replacing the profile deletes the
     * old row. The STUDENT table gets a PROFILE_ID foreign key column.
     *
     * Left at the default fetch type (EAGER for one-to-one) on purpose, to show its cost: loading 2
     * students runs 1 query for the students + 1 per profile (the "N+1 problem", see the topic 4
     * README for the real SQL). The usual fix is @OneToOne(fetch = FetchType.LAZY, ...).
     */
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "profile_id")
    private StudentProfile profile;

    /**
     * Many students, many courses: needs a third table, ENROLLMENT (student_id, course_id).
     * ManyToMany is LAZY by default: the courses are loaded only when the set is first used,
     * and only while the transaction (and the database session) is still open.
     */
    @ManyToMany
    @JoinTable(name = "enrollment",
            joinColumns = @JoinColumn(name = "student_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id"))
    private Set<Course> courses = new HashSet<>();

    protected Student() {
    }

    public Student(String name, String email) {
        this.name = name;
        this.email = email;
    }

    /**
     * Keeps BOTH sides of the relationship in step. Only this side (the owner) is written to the
     * database, but the Java objects should agree too, or course.getStudents() would be out of date.
     */
    public void enroll(Course course) {
        courses.add(course);
        course.getStudents().add(this);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public StudentProfile getProfile() {
        return profile;
    }

    public void setProfile(StudentProfile profile) {
        this.profile = profile;
    }

    public Set<Course> getCourses() {
        return courses;
    }
}
