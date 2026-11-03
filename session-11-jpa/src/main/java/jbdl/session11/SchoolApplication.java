package jbdl.session11;

import jbdl.session11.enrollment.EnrollmentService;
import jbdl.session11.school.StudentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Starts Hibernate on an in-memory H2 database. Hibernate creates the tables from the entity classes
 * (ddl-auto=create-drop), then data.sql adds the sample rows.
 */
@SpringBootApplication
public class SchoolApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchoolApplication.class, args);
    }

    /** Prints a few queries once at startup; the SQL Hibernate generates is logged above each result. */
    @Bean
    CommandLineRunner showSchool(StudentRepository students, EnrollmentService enrollment) {
        return args -> {
            System.out.println("Students: " + students.count());
            System.out.println("Taking Core Java: " + students.findByCoursesTitleOrderByName("Core Java")
                    .stream().map(s -> s.getName()).toList());
            System.out.println("Asha's courses: " + enrollment.courseTitlesOf(1));
        };
    }
}
