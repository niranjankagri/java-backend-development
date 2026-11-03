package jbdl.session11;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jbdl.session11.school.Course;
import jbdl.session11.school.CourseRepository;
import jbdl.session11.school.Student;
import jbdl.session11.school.StudentProfile;
import jbdl.session11.school.StudentRepository;
import jbdl.session11.school.Teacher;
import jbdl.session11.school.TeacherRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Topics 1, 2 and 4: entities are saved and loaded through Spring Data repositories, and the four
 * relationships work. Starting data (data.sql): teachers Mr. Rao and Ms. Iyer; courses Core Java (3
 * seats), Spring Boot (2), SQL Basics (1); Asha (Pune) takes Core Java + Spring Boot, Ravi (Delhi)
 * takes Core Java + SQL Basics.
 *
 * @Transactional: each test is rolled back at the end, so every test starts from that data.
 * entityManager.flush() + clear() forces Hibernate to WRITE now and then forget its cached objects,
 * so the next read really comes from the database.
 */
@SpringBootTest
@Transactional
class EntityAndRepositoryTest {

    @Autowired
    private StudentRepository students;

    @Autowired
    private CourseRepository courses;

    @Autowired
    private TeacherRepository teachers;

    @Autowired
    private EntityManager entityManager;

    private void writeAndForget() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("save() assigns the generated id; findById reads the row back")
    void saveAndLoad() {
        Student saved = students.save(new Student("Kiran", "kiran@example.com"));
        writeAndForget();

        assertThat(saved.getId()).isNotNull();
        assertThat(students.findById(saved.getId())).map(Student::getEmail).contains("kiran@example.com");
    }

    @Test
    @DisplayName("Derived query methods: Spring Data builds the query from the method name")
    void derivedQueries() {
        assertThat(students.findByEmail("ravi@example.com")).map(Student::getName).contains("Ravi");
        assertThat(students.findByNameContainingIgnoreCase("SH")).extracting(Student::getName).containsExactly("Asha");
        assertThat(students.findByCoursesTitleOrderByName("Core Java")).extracting(Student::getName).containsExactly("Asha", "Ravi");
        assertThat(students.countByCoursesTitle("SQL Basics")).isEqualTo(1);
        assertThat(courses.findByTeacherNameOrderByTitle("Mr. Rao")).extracting(Course::getTitle)
                .containsExactly("Core Java", "Spring Boot");
    }

    @Test
    @DisplayName("JPQL @Query: by a field of a related entity (the profile's city)")
    void jpqlQuery() {
        assertThat(students.namesInCity("Pune")).containsExactly("Asha");
    }

    @Test
    @DisplayName("One-to-many / many-to-one: a course knows its teacher, the teacher lists its courses")
    void oneToMany() {
        Teacher rao = courses.findByTitle("Spring Boot").orElseThrow().getTeacher();

        assertThat(rao.getName()).isEqualTo("Mr. Rao");
        assertThat(rao.getCourses()).extracting(Course::getTitle).containsExactlyInAnyOrder("Core Java", "Spring Boot");
    }

    @Test
    @DisplayName("Many-to-many: enroll() writes a row to the join table")
    void manyToMany() {
        Student ravi = students.findByEmail("ravi@example.com").orElseThrow();
        ravi.enroll(courses.findByTitle("Spring Boot").orElseThrow());
        writeAndForget();

        assertThat(students.findByCoursesTitleOrderByName("Spring Boot")).extracting(Student::getName)
                .containsExactly("Asha", "Ravi");
    }

    @Test
    @DisplayName("One-to-one with cascade: saving the student saves the profile; deleting removes it too")
    void oneToOneCascade() {
        Student kiran = new Student("Kiran", "kiran@example.com");
        kiran.setProfile(new StudentProfile("Mumbai", "98000 00003"));
        students.save(kiran); // no separate save for the profile
        writeAndForget();

        Long profileId = students.findById(kiran.getId()).orElseThrow().getProfile().getId();
        assertThat(entityManager.find(StudentProfile.class, profileId).getCity()).isEqualTo("Mumbai");

        students.deleteById(kiran.getId());
        writeAndForget();
        assertThat(entityManager.find(StudentProfile.class, profileId)).isNull(); // gone with the student
    }

    @Test
    @DisplayName("Teacher repository: the inherited CRUD methods need no code at all")
    void inheritedCrud() {
        assertThat(teachers.count()).isEqualTo(2);
        teachers.save(new Teacher("Mrs. Das"));
        assertThat(teachers.findAll()).extracting(Teacher::getName).contains("Mrs. Das");
    }
}
