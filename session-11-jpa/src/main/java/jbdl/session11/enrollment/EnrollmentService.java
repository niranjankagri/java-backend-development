package jbdl.session11.enrollment;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import jbdl.session11.school.Course;
import jbdl.session11.school.CourseRepository;
import jbdl.session11.school.Student;
import jbdl.session11.school.StudentProfile;
import jbdl.session11.school.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactions in Spring: @Transactional makes a method all-or-nothing. Spring (through a proxy, like
 * the aspects of Session 9) starts a transaction before the method and COMMITS after it, or ROLLS BACK
 * if a RuntimeException escapes. Every method below shows one rule.
 */
@Service
public class EnrollmentService {

    private final StudentRepository students;
    private final CourseRepository courses;

    public EnrollmentService(StudentRepository students, CourseRepository courses) {
        this.students = students;
        this.courses = courses;
    }

    /**
     * Enrolls one student. Note: there is no students.save(...) call. Inside a transaction, Hibernate
     * watches the entities it loaded ("managed" entities) and writes their changes at commit time
     * ("dirty checking"): the new row in ENROLLMENT is inserted automatically.
     */
    @Transactional
    public void enroll(long studentId, long courseId) {
        Student student = students.findById(studentId).orElseThrow(() -> new NoSuchElementException("no student " + studentId));
        Course course = courses.findById(courseId).orElseThrow(() -> new NoSuchElementException("no course " + courseId));
        if (course.isFull()) {
            throw new CourseFullException(course.getTitle());
        }
        student.enroll(course);
    }

    /**
     * All or nothing across several steps: create the student AND enroll them in every course. If any
     * course is full, the exception rolls back everything, including the new student row.
     */
    @Transactional
    public Student register(String name, String email, String city, List<String> courseTitles) {
        Student student = new Student(name, email);
        student.setProfile(new StudentProfile(city, null)); // saved with the student (cascade)
        students.save(student);
        for (String title : courseTitles) {
            Course course = courses.findByTitle(title).orElseThrow(() -> new NoSuchElementException("no course " + title));
            if (course.isFull()) {
                throw new CourseFullException(title);
            }
            student.enroll(course);
        }
        return student;
    }

    /**
     * The surprise: rename, then fail with a CHECKED exception. Spring commits anyway, because by
     * default only RuntimeException and Error cause a rollback. The new name is saved.
     */
    @Transactional
    public void renameThenFailChecked(long studentId, String newName) throws NotificationFailedException {
        students.findById(studentId).orElseThrow().setName(newName);
        throw new NotificationFailedException("could not e-mail the student");
    }

    /** The fix: rollbackFor = Exception.class rolls back for checked exceptions too. */
    @Transactional(rollbackFor = Exception.class)
    public void renameThenFailCheckedWithRollback(long studentId, String newName) throws NotificationFailedException {
        students.findById(studentId).orElseThrow().setName(newName);
        throw new NotificationFailedException("could not e-mail the student");
    }

    /**
     * readOnly = true: a hint that nothing will be written. Hibernate skips dirty checking and the
     * database may optimise. The lazy course list can be used here because the transaction is open.
     */
    @Transactional(readOnly = true)
    public List<String> courseTitlesOf(long studentId) {
        Student student = students.findById(studentId).orElseThrow();
        List<String> titles = new ArrayList<>();
        for (Course course : student.getCourses()) { // lazy loading happens here, inside the transaction
            titles.add(course.getTitle());
        }
        titles.sort(null); // natural (alphabetical) order
        return titles;
    }

    /**
     * NO @Transactional: the database session closes as soon as findById returns. Touching the lazy
     * course list afterwards throws LazyInitializationException (shown in the tests). Fixes: a
     * transaction around the work, or fetch what you need in the query (StudentRepository.findWithCourses).
     */
    public Student loadWithoutTransaction(long studentId) {
        return students.findById(studentId).orElseThrow();
    }
}
