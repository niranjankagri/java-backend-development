-- DML (Data Manipulation Language): the starting rows. Runs after schema.sql.
INSERT INTO course (code, title) VALUES ('JAVA', 'Core Java');
INSERT INTO course (code, title) VALUES ('SPRING', 'Spring Boot');

INSERT INTO student (name, email, marks, course_code) VALUES ('Asha',  'asha@example.com',  91, 'JAVA');
INSERT INTO student (name, email, marks, course_code) VALUES ('Ravi',  'ravi@example.com',  72, 'JAVA');
INSERT INTO student (name, email, marks, course_code) VALUES ('Meena', 'meena@example.com', 85, 'SPRING');
