package jbdl.session10.students;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * CRUD with Spring's JdbcTemplate. Compared with PlainJdbcDemo, JdbcTemplate opens and closes the
 * connection, creates the PreparedStatement, sets the ? parameters, loops over the ResultSet and turns
 * SQLException (checked) into Spring's DataAccessException (unchecked). We write only the SQL and the
 * row mapping.
 *
 * @Repository: a Spring bean for data access (also translates database errors into Spring exceptions).
 */
@Repository
public class StudentRepository {

    private static final StudentRowMapper MAPPER = new StudentRowMapper();

    private final JdbcTemplate jdbc;

    /** Spring Boot creates the JdbcTemplate from the DataSource configured in application.properties. */
    public StudentRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---------- Create ----------

    /**
     * INSERT and read back the id the database generated (AUTO_INCREMENT).
     * A plain jdbc.update("INSERT ...", name, email, ...) would insert too, but returns only the row
     * count; getting the new id needs a KeyHolder and our own PreparedStatement, as below.
     */
    public Student create(Student student) {
        KeyHolder keys = new GeneratedKeyHolder(); // JdbcTemplate puts the generated id in here
        // The lambda is a "PreparedStatementCreator": JdbcTemplate hands us a connection, we return the
        // statement to run. RETURN_GENERATED_KEYS asks the driver to send back the new id.
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO student (name, email, marks, course_code) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, student.name());
            statement.setString(2, student.email());
            statement.setInt(3, student.marks());
            statement.setString(4, student.courseCode());
            return statement;
        }, keys);
        return student.withId(keys.getKey().longValue());
    }

    // ---------- Read ----------

    /** query() returns a list: empty if nothing matches. */
    public Optional<Student> findById(long id) {
        List<Student> found = jdbc.query("SELECT * FROM student WHERE id = ?", MAPPER, id);
        // queryForObject(...) exists too, but it THROWS when there is no row; a list is simpler here
        if (found.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(found.get(0));
    }

    public List<Student> findAll() {
        return jdbc.query("SELECT * FROM student ORDER BY id", MAPPER);
    }

    public List<Student> findByCourse(String courseCode) {
        return jdbc.query("SELECT * FROM student WHERE course_code = ? ORDER BY name", MAPPER, courseCode);
    }

    /** A single number: queryForObject with the expected Java type. */
    public int count() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM student", Integer.class);
        return count == null ? 0 : count;
    }

    /** A JOIN that returns plain strings instead of Students: no RowMapper class needed. */
    public List<String> namesWithCourseTitle() {
        return jdbc.query("""
                SELECT s.name, c.title
                FROM student s JOIN course c ON c.code = s.course_code
                ORDER BY s.name""",
                (row, rowNumber) -> row.getString("name") + " - " + row.getString("title"));
    }

    // ---------- Update ----------

    /** Returns true if a row with that id existed (update returns the number of changed rows). */
    public boolean updateMarks(long id, int marks) {
        return jdbc.update("UPDATE student SET marks = ? WHERE id = ?", marks, id) == 1;
    }

    // ---------- Delete ----------

    public boolean deleteById(long id) {
        return jdbc.update("DELETE FROM student WHERE id = ?", id) == 1;
    }
}
