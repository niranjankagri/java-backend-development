package jbdl.session10;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import java.util.UUID;
import jbdl.session10.plainjdbc.PlainJdbcDemo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Topics 1-2: plain JDBC against a fresh in-memory H2 database per test (a random name, so tests never
 * share data). No Spring here at all.
 */
class PlainJdbcDemoTest {

    private PlainJdbcDemo db;

    @BeforeEach
    void freshDatabase() throws SQLException {
        // DB_CLOSE_DELAY=-1 keeps the in-memory database alive between connections of the same test
        db = new PlainJdbcDemo("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        db.createTable();
        db.insert("Clean Code", 464);
        db.insert("Effective Java", 412);
        db.insert("A Short Book", 90);
    }

    @Test
    @DisplayName("INSERT returns the changed row count; SELECT with a parameter reads rows back")
    void insertAndSelect() throws SQLException {
        assertThat(db.insert("Refactoring", 448)).isEqualTo(1);
        assertThat(db.titlesWithMorePagesThan(400)).containsExactly("Clean Code", "Effective Java", "Refactoring");
    }

    @Test
    @DisplayName("SQL injection: glued-in input returns every row; the PreparedStatement doesn't")
    void sqlInjection() throws SQLException {
        String attack = "x' OR '1'='1";

        assertThat(db.countByTitleUnsafe(attack)).isEqualTo(3);  // the WHERE became always-true
        assertThat(db.countByTitleSafe(attack)).isZero();        // looked for a book with that odd title
        assertThat(db.countByTitleSafe("Clean Code")).isEqualTo(1);
    }

    @Test
    @DisplayName("Transaction: when the second insert fails, the first one is rolled back too")
    void rollback() throws SQLException {
        int before = db.count();

        assertThatThrownBy(() -> db.insertBothOrNone("Good Title", null, 100)).isInstanceOf(SQLException.class);

        assertThat(db.count()).isEqualTo(before); // "Good Title" is not there
    }

    @Test
    @DisplayName("Transaction: when both inserts work, both are committed")
    void commit() throws SQLException {
        int before = db.count();

        db.insertBothOrNone("Part 1", "Part 2", 200);

        assertThat(db.count()).isEqualTo(before + 2);
    }
}
