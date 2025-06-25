-- 간단한 더미 데이터 추가

-- 학생 데이터 추가 (10,000명 더 추가하여 총 20,000명)
INSERT INTO school.students (student_number, name, department_id, email, status, enrollment_date, birth_date)
SELECT 
    '2024' || LPAD(n::TEXT, 5, '0'),
    '학생' || n,
    (SELECT department_id FROM school.departments ORDER BY RANDOM() LIMIT 1),
    'student' || (10000 + n) || '@school.ac.kr',
    CASE WHEN RANDOM() < 0.9 THEN '재학' ELSE '휴학' END,
    '2024-03-01'::DATE,
    '2000-01-01'::DATE + (RANDOM() * 2920)::INT
FROM generate_series(1, 10000) AS n;

-- 과목 데이터 추가 (1,000개 더 추가하여 총 3,000개)
INSERT INTO school.courses (course_code, course_name, credits, department_id, professor_id, semester)
SELECT 
    'CS' || LPAD((2000 + n)::TEXT, 4, '0'),
    '과목' || (2000 + n),
    CASE WHEN RANDOM() < 0.6 THEN 3 ELSE 2 END,
    (SELECT department_id FROM school.departments ORDER BY RANDOM() LIMIT 1),
    (SELECT professor_id FROM school.professors ORDER BY RANDOM() LIMIT 1),
    CASE WHEN RANDOM() < 0.5 THEN '2024-1' ELSE '2024-2' END
FROM generate_series(1, 1000) AS n;

-- 수강신청 데이터 추가 (100,000건 더 추가)
INSERT INTO school.enrollments (student_id, course_id, grade, attendance_score, midterm_score, final_score, total_score)
SELECT DISTINCT
    s.student_id,
    c.course_id,
    CASE 
        WHEN RANDOM() < 0.05 THEN 'A+'
        WHEN RANDOM() < 0.15 THEN 'A'
        WHEN RANDOM() < 0.30 THEN 'B+'
        WHEN RANDOM() < 0.50 THEN 'B'
        WHEN RANDOM() < 0.70 THEN 'C+'
        WHEN RANDOM() < 0.85 THEN 'C'
        WHEN RANDOM() < 0.95 THEN 'D'
        ELSE 'F'
    END,
    80 + (RANDOM() * 20)::INT,
    60 + (RANDOM() * 40)::INT,
    60 + (RANDOM() * 40)::INT,
    75 + (RANDOM() * 25)::INT
FROM (SELECT student_id FROM school.students ORDER BY RANDOM() LIMIT 50000) s
CROSS JOIN (SELECT course_id FROM school.courses ORDER BY RANDOM() LIMIT 20) c
LIMIT 100000;

-- 최종 통계
SELECT 
    '학과' as 구분, COUNT(*) as 개수 FROM school.departments
UNION ALL
SELECT 
    '교수', COUNT(*) FROM school.professors  
UNION ALL
SELECT 
    '학생', COUNT(*) FROM school.students
UNION ALL
SELECT 
    '과목', COUNT(*) FROM school.courses
UNION ALL
SELECT 
    '수강신청', COUNT(*) FROM school.enrollments; 