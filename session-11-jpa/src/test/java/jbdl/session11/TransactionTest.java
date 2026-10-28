package jbdl.session11;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;
import jbdl.session11.enrollment.CourseFullException;
import jbdl.session11.enrollment.EnrollmentService;
import jbdl.session11.enrollment.NotificationFailedException;
import jbdl.session11.school.Course;
import jbdl.session11.school.CourseRepository;
import jbdl.session11.school.Student;
import jbdl.session11.school.StudentRepository;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Topic 3: transactional behaviour. This class is deliberately NOT @Transactional: the service's own
 * transactions must really commit or roll back, so the tests can check what reached the database.
 * Because changes stay, every test creates its own course or student (unique names) instead of
 * relying on a count.
 */
@SpringBootTest
class TransactionTest {

    @Autowired
    private EnrollmentService enrollment;

    @Autowired
    private StudentRepository students;

    @Autowired
    private CourseRepository courses;

    private static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private Course newCourse(int capacity) {
        return courses.save(new Course(unique("Course"), capacity, null));
    }

    private Student newStudent() {
        String name = unique("student");
        return students.save(new Student(name, name + "@example.com"));
    }

    @Test
    @DisplayName("Commit: enroll() adds the join-table row without any save() call (dirty checking)")
    void commitByDirtyChecking() {
        Course course = newCourse(5);
        Student student = newStudent();

        enrollment.enroll(student.getId(), course.getId());

        assertThat(enrollment.courseTitlesOf(student.getId())).containsExactly(course.getTitle());
    }

    @Test
    @DisplayName("Rollback: a full course throws, and nothing is enrolled")
    void rollbackOnRuntimeException() {
        Course oneSeat = newCourse(1);
        Student first = newStudent();
        Student second = newStudent();
        enrollment.enroll(first.getId(), oneSeat.getId());

        assertThatThrownBy(() -> enrollment.enroll(second.getId(), oneSeat.getId()))
                .isInstanceOf(CourseFullException.class);
        assertThat(enrollment.courseTitlesOf(second.getId())).isEmpty();
    }

    @Test
    @DisplayName("All or nothing: one full course rolls back the whole registration, student included")
    void rollbackWholeRegistration() {
        Course open = newCourse(10);
        Course full = newCourse(0);
        String email = unique("new") + "@example.com";

        assertThatThrownBy(() -> enrollment.register("New Student", email, "Pune", List.of(open.getTitle(), full.getTitle())))
                .isInstanceOf(CourseFullException.class);

        assertThat(students.findByEmail(email)).isEmpty(); // the INSERT of the student was undone too
    }

    @Test
    @DisplayName("Surprise: a CHECKED exception does not roll back, the new name is committed")
    void checkedExceptionCommits() {
        Student student = newStudent();

        assertThatThrownBy(() -> enrollment.renameThenFailChecked(student.getId(), "Renamed"))
                .isInstanceOf(NotificationFailedException.class);

        assertThat(students.findById(student.getId())).map(Student::getName).contains("Renamed");
    }

    @Test
    @DisplayName("Fix: rollbackFor = Exception.class rolls back for checked exceptions too")
    void rollbackForChecked() {
        Student student = newStudent();

        assertThatThrownBy(() -> enrollment.renameThenFailCheckedWithRollback(student.getId(), "Renamed"))
                .isInstanceOf(NotificationFailedException.class);

        assertThat(students.findById(student.getId())).map(Student::getName).contains(student.getName());
    }

    @Test
    @DisplayName("Lazy loading outside a transaction fails; fetching in the query (join fetch) works")
    void lazyLoading() {
        Student loaded = enrollment.loadWithoutTransaction(1L); // Asha, from data.sql

        assertThatThrownBy(() -> loaded.getCourses().size()).isInstanceOf(LazyInitializationException.class);

        Student fetched = students.findWithCourses(1L).orElseThrow();
        assertThat(fetched.getCourses()).extracting(Course::getTitle).contains("Core Java", "Spring Boot");
    }
}
