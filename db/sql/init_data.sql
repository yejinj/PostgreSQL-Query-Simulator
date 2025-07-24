-- school 스키마 생성
CREATE SCHEMA IF NOT EXISTS school;

-- departments 테이블 생성
CREATE TABLE IF NOT EXISTS school.departments (
    department_id SERIAL PRIMARY KEY,
    department_name VARCHAR(100) NOT NULL
);

-- students 테이블 생성
CREATE TABLE IF NOT EXISTS school.students (
    student_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    student_number VARCHAR(20) UNIQUE NOT NULL,
    department_id INTEGER REFERENCES school.departments(department_id),
    status VARCHAR(20) DEFAULT '재학'
);

-- professors 테이블 생성
CREATE TABLE IF NOT EXISTS school.professors (
    professor_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    department_id INTEGER REFERENCES school.departments(department_id)
);

-- courses 테이블 생성
CREATE TABLE IF NOT EXISTS school.courses (
    course_id SERIAL PRIMARY KEY,
    course_name VARCHAR(100) NOT NULL,
    professor_id INTEGER REFERENCES school.professors(professor_id)
);

-- enrollments 테이블 생성
CREATE TABLE IF NOT EXISTS school.enrollments (
    enrollment_id SERIAL PRIMARY KEY,
    student_id INTEGER REFERENCES school.students(student_id),
    course_id INTEGER REFERENCES school.courses(course_id),
    grade INTEGER CHECK (grade >= 0 AND grade <= 100)
);

-- 샘플 데이터 삽입
-- 부서 데이터
INSERT INTO school.departments (department_name) VALUES 
('컴퓨터공학과'), ('전자공학과'), ('기계공학과'), ('경영학과'), ('영어영문학과')
ON CONFLICT DO NOTHING;

-- 교수 데이터
INSERT INTO school.professors (name, department_id) VALUES 
('김교수', 1), ('이교수', 1), ('박교수', 2), ('최교수', 3), ('정교수', 4)
ON CONFLICT DO NOTHING;

-- 과목 데이터
INSERT INTO school.courses (course_name, professor_id) VALUES 
('데이터베이스시스템', 1), ('자료구조', 2), ('디지털회로', 3), ('기계설계', 4), ('경영학원론', 5)
ON CONFLICT DO NOTHING;

-- 학생 데이터
INSERT INTO school.students (name, student_number, department_id, status) VALUES 
('홍길동', '2023001', 1, '재학'), ('김영희', '2023002', 1, '재학'), 
('이철수', '2023003', 2, '재학'), ('박미영', '2023004', 3, '재학'),
('정현수', '2023005', 4, '재학'), ('강민지', '2023006', 1, '재학'),
('윤성호', '2023007', 2, '재학'), ('임소영', '2023008', 3, '재학'),
('조민수', '2023009', 4, '재학'), ('한지연', '2023010', 5, '재학')
ON CONFLICT DO NOTHING;

-- 수강 데이터
INSERT INTO school.enrollments (student_id, course_id, grade) VALUES 
(1, 1, 85), (1, 2, 90), (2, 1, 78), (2, 2, 82),
(3, 3, 88), (4, 4, 92), (5, 5, 75), (6, 1, 95),
(6, 2, 87), (7, 3, 80), (8, 4, 89), (9, 5, 83),
(10, 1, 91), (3, 1, 77), (4, 2, 94), (5, 1, 68)
ON CONFLICT DO NOTHING; 