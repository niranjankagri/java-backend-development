package jbdl.session11.school;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import java.util.HashSet;
import java.util.Set;

/** A course: taught by one teacher (many-to-one), taken by many students (many-to-many). */
@Entity
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100) // becomes NOT NULL + UNIQUE in the table
    private String title;

    private int capacity;

    /**
     * Many courses, one teacher: this side OWNS the relationship, i.e. the COURSE table gets a
     * TEACHER_ID foreign key column. ManyToOne is loaded EAGERLY by default (with the course).
     */
    @ManyToOne
    private Teacher teacher;

    /** The inverse side of Student.courses; the join table is defined over there. */
    @ManyToMany(mappedBy = "courses")
    private Set<Student> students = new HashSet<>();

    protected Course() {
    }

    public Course(String title, int capacity, Teacher teacher) {
        this.title = title;
        this.capacity = capacity;
        this.teacher = teacher;
    }

    public boolean isFull() {
        return students.size() >= capacity;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getCapacity() {
        return capacity;
    }

    public Teacher getTeacher() {
        return teacher;
    }

    public Set<Student> getStudents() {
        return students;
    }
}
