-- DDL (Data Definition Language): defines the structure. Spring Boot runs this file at startup for
-- embedded databases like H2 (spring.sql.init.mode=embedded is the default).
CREATE TABLE IF NOT EXISTS course (
    code        VARCHAR(10)  PRIMARY KEY,
    title       VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS student (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,                -- the database assigns 1, 2, 3...
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,                     -- no two students share an email
    marks       INT          NOT NULL CHECK (marks BETWEEN 0 AND 100),
    course_code VARCHAR(10)  NOT NULL REFERENCES course (code)    -- foreign key: must exist in course
);
