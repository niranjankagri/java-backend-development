package jbdl.session11.school;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Three ways to ask for data: derived query methods (from the name), JPQL with @Query, and the
 * inherited CRUD methods of JpaRepository.
 */
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByEmail(String email);

    /** Containing = SQL LIKE '%text%'; IgnoreCase = compare in lower case. */
    List<Student> findByNameContainingIgnoreCase(String text);

    /** Follows the many-to-many: students taking the course with this title. */
    List<Student> findByCoursesTitleOrderByName(String courseTitle);

    long countByCoursesTitle(String courseTitle);

    /**
     * JPQL: like SQL but about ENTITIES and their fields (Student, s.courses), not tables and columns.
     * "join fetch" loads the courses in the same query, so they can be used after the transaction ends
     * (no LazyInitializationException) and without one extra query per student (the "N+1 problem").
     */
    @Query("select s from Student s left join fetch s.courses where s.id = :id")
    Optional<Student> findWithCourses(@Param("id") Long id);

    /** JPQL can also return plain values: here the names of students living in a city. */
    @Query("select s.name from Student s where s.profile.city = :city order by s.name")
    List<String> namesInCity(@Param("city") String city);
}
