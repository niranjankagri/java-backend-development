package jbdl.session10.plainjdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC by hand, without Spring: the five steps every database call goes through.
 *
 *   1. get a Connection          DriverManager.getConnection(url, user, password)
 *   2. create a statement        connection.prepareStatement("... ? ...")
 *   3. set the parameters        statement.setString(1, value)
 *   4. execute it                executeQuery() for SELECT, executeUpdate() for INSERT/UPDATE/DELETE/DDL
 *   5. read the ResultSet        while (rs.next()) { rs.getString("name") ... }
 *   ...and close everything      try-with-resources (Session 2) closes in reverse order
 *
 * Every method opens its own connection to keep the example easy to follow. Real applications use a
 * connection POOL (HikariCP, which Spring Boot sets up) because opening a connection is slow.
 */
public class PlainJdbcDemo {

    private final String url;

    /** @param url e.g. "jdbc:h2:mem:demo;DB_CLOSE_DELAY=-1" (keep the in-memory DB while the JVM runs) */
    public PlainJdbcDemo(String url) {
        this.url = url;
    }

    private Connection connect() throws SQLException {
        // The driver is found from the URL prefix "jdbc:h2:" (drivers register themselves automatically)
        return DriverManager.getConnection(url, "sa", "");
    }

    /** DDL with a plain Statement: there are no parameters, so nothing to prepare. */
    public void createTable() throws SQLException {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS book (
                        id     BIGINT AUTO_INCREMENT PRIMARY KEY,
                        title  VARCHAR(100) NOT NULL,
                        pages  INT NOT NULL
                    )""");
        }
    }

    /** INSERT with a PreparedStatement: the ? placeholders are filled safely by the driver. */
    public int insert(String title, int pages) throws SQLException {
        String sql = "INSERT INTO book (title, pages) VALUES (?, ?)";
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, title); // parameters are numbered from 1, not 0
            statement.setInt(2, pages);
            return statement.executeUpdate(); // how many rows changed
        }
    }

    /** SELECT: walk through the ResultSet row by row. */
    public List<String> titlesWithMorePagesThan(int pages) throws SQLException {
        String sql = "SELECT title FROM book WHERE pages > ? ORDER BY title";
        List<String> titles = new ArrayList<>();
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, pages);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {               // moves to the next row; false when there are no more
                    titles.add(rows.getString("title"));
                }
            }
        }
        return titles;
    }

    /**
     * DANGER, for the lesson only: the title is glued into the SQL text. Input such as
     *   x' OR '1'='1
     * changes the meaning of the query (SQL injection) and returns every row.
     */
    public int countByTitleUnsafe(String title) throws SQLException {
        String sql = "SELECT COUNT(*) FROM book WHERE title = '" + title + "'";
        try (Connection connection = connect(); Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            rows.next();
            return rows.getInt(1);
        }
    }

    /** The safe version: with a ? the whole input is treated as ONE value, never as SQL. */
    public int countByTitleSafe(String title) throws SQLException {
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM book WHERE title = ?")) {
            statement.setString(1, title);
            try (ResultSet rows = statement.executeQuery()) {
                rows.next();
                return rows.getInt(1);
            }
        }
    }

    /**
     * A transaction: both inserts succeed, or neither does. By default every statement commits on its
     * own (auto-commit); here we turn that off, commit at the end, and roll back if anything fails.
     */
    public void insertBothOrNone(String firstTitle, String secondTitle, int pages) throws SQLException {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO book (title, pages) VALUES (?, ?)")) {
                statement.setString(1, firstTitle);
                statement.setInt(2, pages);
                statement.executeUpdate();

                statement.setString(1, secondTitle); // a null title breaks the NOT NULL rule -> SQLException
                statement.setInt(2, pages);
                statement.executeUpdate();

                connection.commit();
            } catch (SQLException e) {
                connection.rollback(); // undo the first insert too
                throw e;
            }
        }
    }

    public int count() throws SQLException {
        try (Connection connection = connect(); Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM book")) {
            rows.next();
            return rows.getInt(1);
        }
    }
}
