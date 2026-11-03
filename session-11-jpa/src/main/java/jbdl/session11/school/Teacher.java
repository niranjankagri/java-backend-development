package jbdl.session11.school;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;

/**
 * An ENTITY: a class Hibernate maps to a table (TEACHER), one object per row. Rules for entities:
 * a no-args constructor (protected is enough), an @Id, and not final (Hibernate subclasses it for
 * lazy loading). That's why entities are normal classes, not records.
 */
@Entity
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // the database assigns the id (AUTO_INCREMENT)
    private Long id;

    private String name;

    /**
     * One teacher has many courses. "mappedBy" says: the foreign key lives on the other side
     * (Course.teacher, column TEACHER_ID in the COURSE table); this list is only its mirror.
     */
    @OneToMany(mappedBy = "teacher")
    private List<Course> courses = new ArrayList<>();

    protected Teacher() {
        // for Hibernate: it creates the object first, then fills the fields from the row
    }

    public Teacher(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Course> getCourses() {
        return courses;
    }
}
