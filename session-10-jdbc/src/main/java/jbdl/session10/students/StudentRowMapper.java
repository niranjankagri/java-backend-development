package jbdl.session10.students;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;

/**
 * Turns the CURRENT row of a ResultSet into a Student. JdbcTemplate calls mapRow once per row and
 * collects the results, so we never write the while (rs.next()) loop ourselves.
 *
 * Written as a named class to keep it easy to read. The same mapper as a lambda would be:
 *   (rs, rowNum) -> new Student(rs.getLong("id"), rs.getString("name"), ...)
 */
public class StudentRowMapper implements RowMapper<Student> {

    @Override
    public Student mapRow(ResultSet row, int rowNumber) throws SQLException {
        return new Student(
                row.getLong("id"),
                row.getString("name"),
                row.getString("email"),
                row.getInt("marks"),
                row.getString("course_code")); // column names from schema.sql, Java names in the record
    }
}
