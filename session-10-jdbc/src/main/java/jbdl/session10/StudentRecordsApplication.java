package jbdl.session10;

import jbdl.session10.students.StudentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Starts Spring Boot, which creates the H2 database, runs schema.sql and data.sql, builds a connection
 * pool (HikariCP) and a JdbcTemplate. No web server here: this module is about the database layer.
 */
@SpringBootApplication
public class StudentRecordsApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudentRecordsApplication.class, args);
    }

    /** Runs once after startup: prints what data.sql inserted. */
    @Bean
    CommandLineRunner printStudents(StudentRepository students) {
        return args -> {
            System.out.println("Students: " + students.count());
            students.namesWithCourseTitle().forEach(System.out::println);
        };
    }
}
