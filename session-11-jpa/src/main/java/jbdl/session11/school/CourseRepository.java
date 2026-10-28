package jbdl.session11.school;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Query methods: Spring Data reads the method NAME and writes the query for it. */
public interface CourseRepository extends JpaRepository<Course, Long> {

    /** "find by title" -> select c from Course c where c.title = ?1 */
    Optional<Course> findByTitle(String title);

    /** Follows the relationship: courses whose teacher's name matches, ordered by title. */
    List<Course> findByTeacherNameOrderByTitle(String teacherName);
}
