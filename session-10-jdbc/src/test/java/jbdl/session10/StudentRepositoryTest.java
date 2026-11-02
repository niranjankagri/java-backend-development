package jbdl.session10;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jbdl.session10.students.Student;
import jbdl.session10.students.StudentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Topics 3-5: CRUD with JdbcTemplate on the database Spring Boot built from schema.sql and data.sql
 * (3 students: Asha 91 JAVA, Ravi 72 JAVA, Meena 85 SPRING).
 *
 * @Transactional on a test class: each test runs in a transaction that is ROLLED BACK at the end, so
 * every test starts from exactly those 3 rows, whatever the previous test inserted or deleted.
 */
@SpringBootTest
@Transactional
class StudentRepositoryTest {

    @Autowired
    private StudentRepository students;

    @Test
    @DisplayName("data.sql loaded three students; RowMapper turns each row into a Student")
    void readAll() {
        assertThat(students.count()).isEqualTo(3);
        assertThat(students.findAll()).extracting(Student::name).containsExactly("Asha", "Ravi", "Meena");
    }

    @Test
    @DisplayName("Create: INSERT returns the new student with the id the database generated")
    void create() {
        Student saved = students.create(Student.newStudent("Kiran", "kiran@example.com", 64, "SPRING"));

        assertThat(saved.id()).isNotNull();
        assertThat(students.findById(saved.id())).contains(saved);
        assertThat(students.count()).isEqualTo(4);
    }

    @Test
    @DisplayName("Read: by id (found / not found), by course, and a JOIN")
    void read() {
        assertThat(students.findById(1)).map(Student::name).contains("Asha");
        assertThat(students.findById(999)).isEmpty();
        assertThat(students.findByCourse("JAVA")).extracting(Student::name).containsExactly("Asha", "Ravi");
        assertThat(students.namesWithCourseTitle()).contains("Meena - Spring Boot");
    }

    @Test
    @DisplayName("Update: changes one row; an unknown id changes none")
    void update() {
        assertThat(students.updateMarks(2, 80)).isTrue();
        assertThat(students.findById(2)).map(Student::marks).contains(80);
        assertThat(students.updateMarks(999, 80)).isFalse();
    }

    @Test
    @DisplayName("Delete: removes one row; an unknown id removes none")
    void delete() {
        assertThat(students.deleteById(3)).isTrue();
        assertThat(students.count()).isEqualTo(2);
        assertThat(students.deleteById(3)).isFalse();
    }

    @Test
    @DisplayName("The database enforces the DDL rules: UNIQUE email, CHECK on marks, foreign key on course")
    void constraints() {
        assertThatThrownBy(() -> students.create(Student.newStudent("Copy", "asha@example.com", 50, "JAVA")))
                .isInstanceOf(DataIntegrityViolationException.class);   // UNIQUE
        assertThatThrownBy(() -> students.create(Student.newStudent("Too High", "x@example.com", 150, "JAVA")))
                .isInstanceOf(DataIntegrityViolationException.class);   // CHECK (marks BETWEEN 0 AND 100)
        assertThatThrownBy(() -> students.create(Student.newStudent("No Course", "y@example.com", 50, "PYTHON")))
                .isInstanceOf(DataIntegrityViolationException.class);   // REFERENCES course (code)
    }
}
