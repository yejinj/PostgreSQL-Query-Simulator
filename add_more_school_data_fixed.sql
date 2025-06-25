-- school 스키마에 더 많은 더미 데이터 추가 (수정된 버전)

-- 추가 학생 데이터 (기존 10,000명에서 50,000명으로 확장)
DO $$
DECLARE
    dept_ids INT[];
    selected_dept_id INT;
    student_name VARCHAR(100);
    surnames VARCHAR[] := ARRAY['김', '이', '박', '최', '정', '강', '조', '윤', '장', '임', '한', '오', '서', '신', '권', '전', '송', '안', '홍', '문'];
    firstnames VARCHAR[] := ARRAY['민수', '영희', '철수', '영수', '미영', '정호', '은정', '준호', '지혜', '성민', '소영', '동혁', '혜진', '태윤', '수진', '지훈', '예원', '도현', '서연', '현우'];
BEGIN
    -- 모든 학과 ID 배열로 가져오기
    SELECT ARRAY(SELECT department_id FROM school.departments) INTO dept_ids;
    
    -- 40,000명의 학생 추가
    FOR i IN 10002..50001 LOOP
        selected_dept_id := dept_ids[1 + (RANDOM() * array_length(dept_ids, 1))::INT];
        student_name := surnames[1 + (RANDOM() * 20)::INT] || firstnames[1 + (RANDOM() * 20)::INT];
        
        INSERT INTO school.students (student_number, name, department_id, email, status, enrollment_date, birth_date)
        VALUES (
            CASE 
                WHEN i <= 25000 THEN '2024' || LPAD((i-10001)::TEXT, 5, '0')
                WHEN i <= 40000 THEN '2023' || LPAD((i-25001)::TEXT, 5, '0')
                ELSE '2022' || LPAD((i-40001)::TEXT, 5, '0')
            END,
            student_name,
            selected_dept_id,
            'student' || i || '@school.ac.kr',
            CASE 
                WHEN RANDOM() < 0.88 THEN '재학'
                WHEN RANDOM() < 0.95 THEN '휴학'
                ELSE '졸업'
            END,
            CASE 
                WHEN i <= 25000 THEN '2024-03-01'::DATE
                WHEN i <= 40000 THEN '2023-03-01'::DATE
                ELSE '2022-03-01'::DATE
            END,
            '1998-01-01'::DATE + (RANDOM() * 3650)::INT  -- 1998-2008년생
        );
        
        -- 진행상황 출력 (5000명마다)
        IF i % 5000 = 0 THEN
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
    course_names VARCHAR[] := ARRAY[
        '머신러닝기초', 'AI수학', '딥러닝프로그래밍', '자연어처리', '컴퓨터비전', '강화학습', 'AI윤리', '데이터마이닝', '빅데이터분석', '텐서플로우실습',
        '파이토치기초', 'AI알고리즘', '지식그래프', '추천시스템', '음성인식', '이미지처리', 'AI플랫폼', '딥러닝이론', '신경망구조', 'AutoML',
        '정보보안개론', '네트워크보안', '시스템보안', '웹보안', '모바일보안', '암호학', '해킹방어', '디지털포렌식', '보안감사', 'CISO실무',
        '침입탐지', '방화벽관리', '취약점분석', '보안관제', '개인정보보호', '블록체인보안', '클라우드보안', 'IoT보안', '산업보안', '사이버위협분석',
        '데이터베이스설계', '소프트웨어공학', '알고리즘분석', '컴퓨터구조', '운영체제', '네트워크프로그래밍', '웹프로그래밍', '모바일프로그래밍', '게임개발', '앱개발'
    ];
BEGIN
    SELECT ARRAY(SELECT professor_id FROM school.professors) INTO prof_ids;
    SELECT ARRAY(SELECT department_id FROM school.departments) INTO dept_ids;
    
    -- 500개의 과목 추가
    FOR i IN 1..500 LOOP
        selected_prof_id := prof_ids[1 + (RANDOM() * array_length(prof_ids, 1))::INT];
        selected_dept_id := dept_ids[1 + (RANDOM() * array_length(dept_ids, 1))::INT];
        
        INSERT INTO school.courses (course_code, course_name, credits, department_id, professor_id, semester)
        VALUES (
            'CS' || LPAD(course_counter::TEXT, 4, '0'),
            course_names[1 + (RANDOM() * array_length(course_names, 1))::INT] || i,
            CASE WHEN RANDOM() < 0.6 THEN 3 ELSE 2 END,
            selected_dept_id,
            selected_prof_id,
            CASE WHEN RANDOM() < 0.5 THEN '2024-1' ELSE '2024-2' END
        );
        course_counter := course_counter + 1;
        
        -- 진행상황 출력 (100개마다)
        IF i % 100 = 0 THEN
            RAISE NOTICE '과목 데이터 생성 진행: %개', i;
        END IF;
    END LOOP;
END $$;

-- 추가 수강신청 데이터 (기존 30,000건에서 200,000건으로 확장)
DO $$
DECLARE
    student_ids INT[];
    course_ids INT[];
    selected_student_id INT;
    selected_course_id INT;
    enrollment_counter INT := 0;
BEGIN
    SELECT ARRAY(SELECT student_id FROM school.students) INTO student_ids;
    SELECT ARRAY(SELECT course_id FROM school.courses) INTO course_ids;
    
    -- 170,000건의 수강신청 추가
    FOR i IN 1..170000 LOOP
        selected_student_id := student_ids[1 + (RANDOM() * array_length(student_ids, 1))::INT];
        selected_course_id := course_ids[1 + (RANDOM() * array_length(course_ids, 1))::INT];
        
        -- 중복 수강신청 방지
        IF NOT EXISTS (
            SELECT 1 FROM school.enrollments 
            WHERE student_id = selected_student_id AND course_id = selected_course_id
        ) THEN
            INSERT INTO school.enrollments (student_id, course_id, grade, attendance_score, midterm_score, final_score, total_score)
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
                60 + (RANDOM() * 40)::INT,  -- 60~100점
                (60 + (RANDOM() * 40)::INT + 60 + (RANDOM() * 40)::INT + 80 + (RANDOM() * 20)::INT) / 3  -- 평균
            );
            enrollment_counter := enrollment_counter + 1;
        END IF;
        
        -- 진행상황 출력 (10000건마다)
        IF i % 10000 = 0 THEN
            RAISE NOTICE '수강신청 데이터 생성 진행: %건', enrollment_counter;
        END IF;
    END LOOP;
END $$;

-- 현재 데이터 통계 확인
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