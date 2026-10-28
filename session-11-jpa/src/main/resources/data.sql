-- Sample rows. Table and column names are the ones Hibernate generated from the entities
-- (class Course -> table COURSE, field capacity -> column CAPACITY, Course.teacher -> TEACHER_ID ...).
INSERT INTO teacher (name) VALUES ('Mr. Rao');
INSERT INTO teacher (name) VALUES ('Ms. Iyer');

INSERT INTO course (title, capacity, teacher_id) VALUES ('Core Java', 3, 1);
INSERT INTO course (title, capacity, teacher_id) VALUES ('Spring Boot', 2, 1);
INSERT INTO course (title, capacity, teacher_id) VALUES ('SQL Basics', 1, 2);

INSERT INTO student_profile (city, phone) VALUES ('Pune', '98000 00001');
INSERT INTO student_profile (city, phone) VALUES ('Delhi', '98000 00002');

INSERT INTO student (name, email, profile_id) VALUES ('Asha', 'asha@example.com', 1);
INSERT INTO student (name, email, profile_id) VALUES ('Ravi', 'ravi@example.com', 2);

-- Asha takes Core Java and Spring Boot; Ravi takes Core Java and the 1-seat SQL Basics (now full)
INSERT INTO enrollment (student_id, course_id) VALUES (1, 1);
INSERT INTO enrollment (student_id, course_id) VALUES (1, 2);
INSERT INTO enrollment (student_id, course_id) VALUES (2, 1);
INSERT INTO enrollment (student_id, course_id) VALUES (2, 3);
