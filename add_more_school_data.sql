-- school 스키마에 더 많은 더미 데이터 추가

-- 추가 학과 데이터
INSERT INTO school.departments (department_name, building, phone) VALUES
('AI융합학과', 'AI관', '02-1234-5678'),
('데이터사이언스학과', 'DS관', '02-1234-5679'),
('사이버보안학과', '보안관', '02-1234-5680'),
('소프트웨어공학과', 'SW관', '02-1234-5681'),
('게임개발학과', '게임관', '02-1234-5682'),
('모바일앱개발학과', '모바일관', '02-1234-5683'),
('클라우드컴퓨팅학과', '클라우드관', '02-1234-5684'),
('블록체인학과', '블록체인관', '02-1234-5685'),
('로봇공학과', '로봇관', '02-1234-5686'),
('바이오메디컬공학과', '바이오관', '02-1234-5687');

-- 추가 교수 데이터 (최신 학과들을 위한)
DO $$
DECLARE
    dept_id INT;
    prof_counter INT := 1001;
BEGIN
    -- AI융합학과 교수들
    SELECT department_id INTO dept_id FROM school.departments WHERE department_name = 'AI융합학과';
    FOR i IN 1..15 LOOP
        INSERT INTO school.professors (professor_number, name, department_id, title, hire_date)
        VALUES (
            'P' || LPAD(prof_counter::TEXT, 6, '0'),
            '김AI교수' || i,
            dept_id,
            CASE WHEN i <= 5 THEN '교수'
                 WHEN i <= 10 THEN '부교수'
                 ELSE '조교수' END,
            '2020-01-01'::DATE + (RANDOM() * 1460)::INT
        );
        prof_counter := prof_counter + 1;
    END LOOP;

    -- 데이터사이언스학과 교수들
    SELECT department_id INTO dept_id FROM school.departments WHERE department_name = '데이터사이언스학과';
    FOR i IN 1..12 LOOP
        INSERT INTO school.professors (professor_number, name, department_id, title, hire_date)
        VALUES (
            'P' || LPAD(prof_counter::TEXT, 6, '0'),
            '박데이터교수' || i,
            dept_id,
            CASE WHEN i <= 4 THEN '교수'
                 WHEN i <= 8 THEN '부교수'
                 ELSE '조교수' END,
            '2019-01-01'::DATE + (RANDOM() * 1460)::INT
        );
        prof_counter := prof_counter + 1;
    END LOOP;

    -- 사이버보안학과 교수들
    SELECT department_id INTO dept_id FROM school.departments WHERE department_name = '사이버보안학과';
    FOR i IN 1..10 LOOP
        INSERT INTO school.professors (professor_number, name, department_id, title, hire_date)
        VALUES (
            'P' || LPAD(prof_counter::TEXT, 6, '0'),
            '이보안교수' || i,
            dept_id,
            CASE WHEN i <= 3 THEN '교수'
                 WHEN i <= 6 THEN '부교수'
                 ELSE '조교수' END,
            '2021-01-01'::DATE + (RANDOM() * 1095)::INT
        );
        prof_counter := prof_counter + 1;
    END LOOP;
END $$;

-- 추가 학생 데이터 (기존 10,000명에서 20,000명으로 확장)
DO $$
DECLARE
    dept_ids INT[];
    selected_dept_id INT;
BEGIN
    -- 모든 학과 ID 배열로 가져오기
    SELECT ARRAY(SELECT department_id FROM school.departments) INTO dept_ids;
    
    -- 10,000명의 학생 추가
    FOR i IN 10002..20001 LOOP
        selected_dept_id := dept_ids[1 + (RANDOM() * array_length(dept_ids, 1))::INT];
        
        INSERT INTO school.students (student_number, name, department_id, email, status, enrollment_date, birth_date)
        VALUES (
            CASE 
                WHEN i <= 15000 THEN '2024' || LPAD((i-10001)::TEXT, 5, '0')
                ELSE '2023' || LPAD((i-15001)::TEXT, 5, '0')
            END,
            CASE 
                WHEN RANDOM() < 0.5 THEN 
                    (ARRAY['김', '이', '박', '최', '정', '강', '조', '윤', '장', '임', '한', '오', '서', '신', '권'])[1 + (RANDOM() * 15)::INT] ||
                    (ARRAY['민수', '영희', '철수', '영수', '미영', '정호', '은정', '준호', '지혜', '성민', '소영', '동혁', '혜진', '태윤', '수진'])[1 + (RANDOM() * 15)::INT]
                ELSE
                    (ARRAY['신', '유', '노', '하', '배', '전', '송', '안', '홍', '문', '양', '구', '남', '심', '원'])[1 + (RANDOM() * 15)::INT] ||
                    (ARRAY['지훈', '예원', '도현', '서연', '현우', '나연', '민지', '재원', '수빈', '예준', '아영', '건우', '다현', '시우', '하은'])[1 + (RANDOM() * 15)::INT]
            END,
            selected_dept_id,
            'student' || i || '@school.ac.kr',
            CASE 
                WHEN RANDOM() < 0.9 THEN 'active'
                WHEN RANDOM() < 0.95 THEN 'leave'
                ELSE 'graduate'
            END,
            CASE 
                WHEN i <= 15000 THEN '2024-03-01'::DATE
                ELSE '2023-03-01'::DATE
            END,
            '2000-01-01'::DATE + (RANDOM() * 2920)::INT  -- 2000-2008년생
        );
        
        -- 진행상황 출력 (1000명마다)
        IF i % 1000 = 0 THEN
            RAISE NOTICE '학생 데이터 생성 진행: %명', i-10001;
        END IF;
    END LOOP;
END $$;

-- 추가 과목 데이터
DO $$
DECLARE
    prof_ids INT[];
    dept_ids INT[];
    selected_prof_id INT;
    selected_dept_id INT;
    course_counter INT := 3001;
BEGIN
    SELECT ARRAY(SELECT professor_id FROM school.professors) INTO prof_ids;
    SELECT ARRAY(SELECT department_id FROM school.departments) INTO dept_ids;
    
    -- AI/데이터 관련 과목들
    FOR i IN 1..50 LOOP
        selected_prof_id := prof_ids[1 + (RANDOM() * array_length(prof_ids, 1))::INT];
        selected_dept_id := dept_ids[1 + (RANDOM() * array_length(dept_ids, 1))::INT];
        
        INSERT INTO school.courses (course_code, course_name, credits, department_id, professor_id, semester)
        VALUES (
            'AI' || LPAD(course_counter::TEXT, 4, '0'),
            (ARRAY['머신러닝기초', 'AI수학', '딥러닝프로그래밍', '자연어처리', '컴퓨터비전', '강화학습', 'AI윤리', '데이터마이닝', '빅데이터분석', '텐서플로우실습',
                   '파이토치기초', 'AI알고리즘', '지식그래프', '추천시스템', '음성인식', '이미지처리', 'AI플랫폼', '딥러닝이론', '신경망구조', 'AutoML'])[1 + (RANDOM() * 20)::INT],
            CASE WHEN RANDOM() < 0.6 THEN 3 ELSE 2 END,
            selected_dept_id,
            selected_prof_id,
            CASE WHEN RANDOM() < 0.5 THEN '2024-1' ELSE '2024-2' END
        );
        course_counter := course_counter + 1;
    END LOOP;
    
    -- 보안 관련 과목들
    FOR i IN 1..30 LOOP
        selected_prof_id := prof_ids[1 + (RANDOM() * array_length(prof_ids, 1))::INT];
        selected_dept_id := dept_ids[1 + (RANDOM() * array_length(dept_ids, 1))::INT];
        
        INSERT INTO school.courses (course_code, course_name, credits, department_id, professor_id, semester)
        VALUES (
            'SEC' || LPAD(course_counter::TEXT, 4, '0'),
            (ARRAY['정보보안개론', '네트워크보안', '시스템보안', '웹보안', '모바일보안', '암호학', '해킹방어', '디지털포렌식', '보안감사', 'CISO실무',
                   '침입탐지', '방화벽관리', '취약점분석', '보안관제', '개인정보보호', '블록체인보안', '클라우드보안', 'IoT보안', '산업보안', '사이버위협분석'])[1 + (RANDOM() * 20)::INT],
            CASE WHEN RANDOM() < 0.7 THEN 3 ELSE 2 END,
            selected_dept_id,
            selected_prof_id,
            CASE WHEN RANDOM() < 0.5 THEN '2024-1' ELSE '2024-2' END
        );
        course_counter := course_counter + 1;
    END LOOP;
END $$;

-- 추가 수강신청 데이터 (기존 30,000건에서 100,000건으로 확장)
DO $$
DECLARE
    student_ids INT[];
    course_ids INT[];
    selected_student_id INT;
    selected_course_id INT;
    enrollment_counter INT := 30001;
BEGIN
    SELECT ARRAY(SELECT student_id FROM school.students) INTO student_ids;
    SELECT ARRAY(SELECT course_id FROM school.courses) INTO course_ids;
    
    -- 70,000건의 수강신청 추가
    FOR i IN 1..70000 LOOP
        selected_student_id := student_ids[1 + (RANDOM() * array_length(student_ids, 1))::INT];
        selected_course_id := course_ids[1 + (RANDOM() * array_length(course_ids, 1))::INT];
        
        -- 중복 수강신청 방지
        IF NOT EXISTS (
            SELECT 1 FROM school.enrollments 
            WHERE student_id = selected_student_id AND course_id = selected_course_id
        ) THEN
            INSERT INTO school.enrollments (student_id, course_id, grade, attendance_score, total_score, enrollment_date)
            VALUES (
                selected_student_id,
                selected_course_id,
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
                80 + (RANDOM() * 20)::INT,  -- 80~100점
                60 + (RANDOM() * 40)::INT,  -- 60~100점
                '2024-03-01'::DATE + (RANDOM() * 60)::INT
            );
            enrollment_counter := enrollment_counter + 1;
        END IF;
        
        -- 진행상황 출력 (5000건마다)
        IF i % 5000 = 0 THEN
            RAISE NOTICE '수강신청 데이터 생성 진행: %건', i;
        END IF;
    END LOOP;
END $$;

-- 최종 통계 출력
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