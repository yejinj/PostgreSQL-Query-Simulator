-- 기존 데이터 삭제 (외래키 순서를 고려해서)
DELETE FROM school.enrollments;
DELETE FROM school.courses;
DELETE FROM school.professors;
DELETE FROM school.students;

-- 교수 데이터 삽입 (professor_number 필수)
INSERT INTO school.professors (professor_number, name, department_id, title) VALUES 
('P001', '김교수', 1, '교수'),
('P002', '이교수', 1, '부교수'),
('P003', '박교수', 2, '교수'),
('P004', '최교수', 3, '부교수'),
('P005', '정교수', 4, '교수');

-- 과목 데이터 삽입 (course_code 필수)
INSERT INTO school.courses (course_code, course_name, professor_id, department_id, credits) VALUES 
('CS101', '데이터베이스시스템', 1, 1, 3),
('CS102', '자료구조', 2, 1, 3),
('EE101', '디지털회로', 3, 2, 3),
('ME101', '기계설계', 4, 3, 3),
('BA101', '경영학원론', 5, 4, 3);

-- 학생 데이터 삽입
INSERT INTO school.students (student_number, name, department_id, status) VALUES 
('2023001', '홍길동', 1, '재학'),
('2023002', '김영희', 1, '재학'),
('2023003', '이철수', 2, '재학'),
('2023004', '박미영', 3, '재학'),
('2023005', '정현수', 4, '재학'),
('2023006', '강민지', 1, '재학'),
('2023007', '윤성호', 2, '재학'),
('2023008', '임소영', 3, '재학'),
('2023009', '조민수', 4, '재학'),
('2023010', '한지연', 5, '재학');

-- 수강 데이터 삽입
INSERT INTO school.enrollments (student_id, course_id, grade) VALUES 
(1, 1, 85), (1, 2, 90), (2, 1, 78), (2, 2, 82),
(3, 3, 88), (4, 4, 92), (5, 5, 75), (6, 1, 95),
(6, 2, 87), (7, 3, 80), (8, 4, 89), (9, 5, 83),
(10, 1, 91), (3, 1, 77), (4, 2, 94), (5, 1, 68); 