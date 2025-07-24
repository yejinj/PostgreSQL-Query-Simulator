-- AI 병목 분석 테스트를 위한 비효율적인 쿼리들

-- 1. 전체 테이블 스캔을 유발하는 쿼리 (인덱스가 없는 컬럼으로 검색)
SELECT * FROM school.students WHERE name LIKE '%김%';

-- 2. 카티시안 조인을 유발하는 쿼리 (ON 절 없음)
SELECT s.name, d.department_name 
FROM school.students s, school.departments d 
WHERE s.student_id < 5;

-- 3. 비효율적인 서브쿼리
SELECT s.name, s.student_number,
       (SELECT COUNT(*) FROM school.enrollments e WHERE e.student_id = s.student_id) as enrollment_count
FROM school.students s
WHERE s.department_id IN (
    SELECT d.department_id 
    FROM school.departments d 
    WHERE d.department_name LIKE '%공학%'
);

-- 4. 함수를 WHERE 절에서 사용하는 비효율적인 쿼리
SELECT * FROM school.students 
WHERE UPPER(name) = 'HONG';

-- 5. ORDER BY 없이 LIMIT 사용 (불안정한 결과)
SELECT * FROM school.students LIMIT 10; 