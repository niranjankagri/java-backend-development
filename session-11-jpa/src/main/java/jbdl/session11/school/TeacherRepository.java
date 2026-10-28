package jbdl.session11.school;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * A Spring Data repository: only an interface. Spring generates the class at startup, with save,
 * findById, findAll, count, deleteById and more, for the entity Teacher with ids of type Long.
 */
public interface TeacherRepository extends JpaRepository<Teacher, Long> {
}
