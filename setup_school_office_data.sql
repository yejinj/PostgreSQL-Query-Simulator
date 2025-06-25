-- 학교/직장 스키마 및 대용량 더미 데이터 생성 스크립트

-- 기존 스키마 삭제 (있다면)
DROP SCHEMA IF EXISTS school CASCADE;
DROP SCHEMA IF EXISTS office CASCADE;

-- 새 스키마 생성
CREATE SCHEMA school;
CREATE SCHEMA office;

-- === 학교 스키마 ===
-- 학과 테이블
CREATE TABLE school.departments (
    department_id SERIAL PRIMARY KEY,
    department_name VARCHAR(100) NOT NULL,
    building VARCHAR(50),
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 학생 테이블
CREATE TABLE school.students (
    student_id SERIAL PRIMARY KEY,
    student_number VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    department_id INTEGER REFERENCES school.departments(department_id),
    email VARCHAR(150),
    phone VARCHAR(20),
    address TEXT,
    birth_date DATE,
    enrollment_date DATE DEFAULT CURRENT_DATE,
    status VARCHAR(20) DEFAULT '재학',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 교수 테이블
CREATE TABLE school.professors (
    professor_id SERIAL PRIMARY KEY,
    professor_number VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    department_id INTEGER REFERENCES school.departments(department_id),
    email VARCHAR(150),
    phone VARCHAR(20),
    office_location VARCHAR(100),
    title VARCHAR(50),
    hire_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 과목 테이블
CREATE TABLE school.courses (
    course_id SERIAL PRIMARY KEY,
    course_code VARCHAR(20) UNIQUE NOT NULL,
    course_name VARCHAR(200) NOT NULL,
    credits INTEGER DEFAULT 3,
    department_id INTEGER REFERENCES school.departments(department_id),
    professor_id INTEGER REFERENCES school.professors(professor_id),
    semester VARCHAR(20),
    year INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 수강신청 테이블
CREATE TABLE school.enrollments (
    enrollment_id SERIAL PRIMARY KEY,
    student_id INTEGER REFERENCES school.students(student_id),
    course_id INTEGER REFERENCES school.courses(course_id),
    grade VARCHAR(5),
    attendance_score INTEGER DEFAULT 0,
    midterm_score INTEGER DEFAULT 0,
    final_score INTEGER DEFAULT 0,
    total_score INTEGER DEFAULT 0,
    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(student_id, course_id)
);

-- === 직장 스키마 ===
-- 부서 테이블
CREATE TABLE office.departments (
    department_id SERIAL PRIMARY KEY,
    department_name VARCHAR(100) NOT NULL,
    location VARCHAR(100),
    budget DECIMAL(15,2),
    manager_id INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 직원 테이블
CREATE TABLE office.employees (
    employee_id SERIAL PRIMARY KEY,
    employee_number VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    department_id INTEGER REFERENCES office.departments(department_id),
    position VARCHAR(50),
    email VARCHAR(150),
    phone VARCHAR(20),
    salary DECIMAL(12,2),
    hire_date DATE,
    status VARCHAR(20) DEFAULT '재직',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 프로젝트 테이블
CREATE TABLE office.projects (
    project_id SERIAL PRIMARY KEY,
    project_name VARCHAR(200) NOT NULL,
    description TEXT,
    start_date DATE,
    end_date DATE,
    budget DECIMAL(15,2),
    status VARCHAR(20) DEFAULT '진행중',
    manager_id INTEGER REFERENCES office.employees(employee_id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 프로젝트 참여 테이블
CREATE TABLE office.project_assignments (
    assignment_id SERIAL PRIMARY KEY,
    project_id INTEGER REFERENCES office.projects(project_id),
    employee_id INTEGER REFERENCES office.employees(employee_id),
    role VARCHAR(50),
    hours_per_week INTEGER DEFAULT 40,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(project_id, employee_id)
);

-- 급여 테이블
CREATE TABLE office.salaries (
    salary_id SERIAL PRIMARY KEY,
    employee_id INTEGER REFERENCES office.employees(employee_id),
    base_salary DECIMAL(12,2),
    bonus DECIMAL(12,2) DEFAULT 0,
    overtime_pay DECIMAL(12,2) DEFAULT 0,
    total_amount DECIMAL(12,2),
    pay_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- === 학교 더미 데이터 생성 ===
-- 학과 데이터 (50개)
INSERT INTO school.departments (department_name, building, phone)
SELECT 
    CASE (i % 10)
        WHEN 0 THEN '컴퓨터공학과'
        WHEN 1 THEN '전자공학과'
        WHEN 2 THEN '기계공학과'
        WHEN 3 THEN '경영학과'
        WHEN 4 THEN '경제학과'
        WHEN 5 THEN '영어영문학과'
        WHEN 6 THEN '수학과'
        WHEN 7 THEN '물리학과'
        WHEN 8 THEN '화학과'
        WHEN 9 THEN '생물학과'
    END || ' ' || ((i / 10) + 1) || '전공',
    '공학관 ' || (i % 5 + 1) || '동',
    '02-' || LPAD((2000 + i)::text, 4, '0') || '-' || LPAD((i % 10000)::text, 4, '0')
FROM generate_series(1, 50) i;

-- 교수 데이터 (1000명)
INSERT INTO school.professors (professor_number, name, department_id, email, phone, office_location, title, hire_date)
SELECT 
    'P' || LPAD(i::text, 6, '0'),
    CASE (i % 20)
        WHEN 0 THEN '김교수'
        WHEN 1 THEN '이교수'
        WHEN 2 THEN '박교수'
        WHEN 3 THEN '정교수'
        WHEN 4 THEN '최교수'
        WHEN 5 THEN '조교수'
        WHEN 6 THEN '윤교수'
        WHEN 7 THEN '장교수'
        WHEN 8 THEN '임교수'
        WHEN 9 THEN '한교수'
        WHEN 10 THEN '오교수'
        WHEN 11 THEN '서교수'
        WHEN 12 THEN '신교수'
        WHEN 13 THEN '권교수'
        WHEN 14 THEN '황교수'
        WHEN 15 THEN '안교수'
        WHEN 16 THEN '송교수'
        WHEN 17 THEN '전교수'
        WHEN 18 THEN '홍교수'
        WHEN 19 THEN '고교수'
    END || i,
    (i % 50) + 1,
    'prof' || i || '@university.ac.kr',
    '010-' || LPAD((1000 + i % 9000)::text, 4, '0') || '-' || LPAD((i % 10000)::text, 4, '0'),
    '연구실 ' || (i % 500 + 1),
    CASE (i % 4)
        WHEN 0 THEN '교수'
        WHEN 1 THEN '부교수'
        WHEN 2 THEN '조교수'
        WHEN 3 THEN '전임강사'
    END,
    CURRENT_DATE - (i % 3650)::integer
FROM generate_series(1, 1000) i;

-- 학생 데이터 (30000명)
INSERT INTO school.students (student_number, name, department_id, email, phone, address, birth_date, enrollment_date, status)
SELECT 
    (2020 + (i % 5))::text || LPAD(((i % 50) + 1)::text, 2, '0') || LPAD((i % 10000)::text, 4, '0'),
    CASE (i % 30)
        WHEN 0 THEN '김학생'
        WHEN 1 THEN '이학생'
        WHEN 2 THEN '박학생'
        WHEN 3 THEN '정학생'
        WHEN 4 THEN '최학생'
        WHEN 5 THEN '조학생'
        WHEN 6 THEN '윤학생'
        WHEN 7 THEN '장학생'
        WHEN 8 THEN '임학생'
        WHEN 9 THEN '한학생'
        WHEN 10 THEN '오학생'
        WHEN 11 THEN '서학생'
        WHEN 12 THEN '신학생'
        WHEN 13 THEN '권학생'
        WHEN 14 THEN '황학생'
        WHEN 15 THEN '안학생'
        WHEN 16 THEN '송학생'
        WHEN 17 THEN '전학생'
        WHEN 18 THEN '홍학생'
        WHEN 19 THEN '고학생'
        WHEN 20 THEN '문학생'
        WHEN 21 THEN '양학생'
        WHEN 22 THEN '배학생'
        WHEN 23 THEN '백학생'
        WHEN 24 THEN '허학생'
        WHEN 25 THEN '유학생'
        WHEN 26 THEN '남학생'
        WHEN 27 THEN '심학생'
        WHEN 28 THEN '노학생'
        WHEN 29 THEN '하학생'
    END || i,
    (i % 50) + 1,
    'student' || i || '@student.ac.kr',
    '010-' || LPAD((1000 + i % 9000)::text, 4, '0') || '-' || LPAD((i % 10000)::text, 4, '0'),
    '서울시 ' || CASE (i % 5)
        WHEN 0 THEN '강남구'
        WHEN 1 THEN '서초구'
        WHEN 2 THEN '송파구'
        WHEN 3 THEN '관악구'
        WHEN 4 THEN '동작구'
    END || ' ' || (i % 100 + 1) || '번지',
    CURRENT_DATE - (365 * 20 + i % 1825)::integer,
    CURRENT_DATE - (365 * (i % 5) + i % 365)::integer,
    CASE (i % 10)
        WHEN 0 THEN '휴학'
        WHEN 1 THEN '졸업'
        ELSE '재학'
    END
FROM generate_series(1, 30000) i;

-- 과목 데이터 (2000개)
INSERT INTO school.courses (course_code, course_name, credits, department_id, professor_id, semester, year)
SELECT 
    CASE (i % 10)
        WHEN 0 THEN 'CS'
        WHEN 1 THEN 'EE'
        WHEN 2 THEN 'ME'
        WHEN 3 THEN 'BA'
        WHEN 4 THEN 'EC'
        WHEN 5 THEN 'EN'
        WHEN 6 THEN 'MA'
        WHEN 7 THEN 'PH'
        WHEN 8 THEN 'CH'
        WHEN 9 THEN 'BI'
    END || LPAD(i::text, 4, '0'),
    CASE (i % 15)
        WHEN 0 THEN '데이터구조'
        WHEN 1 THEN '알고리즘'
        WHEN 2 THEN '데이터베이스'
        WHEN 3 THEN '운영체제'
        WHEN 4 THEN '네트워크'
        WHEN 5 THEN '소프트웨어공학'
        WHEN 6 THEN '인공지능'
        WHEN 7 THEN '머신러닝'
        WHEN 8 THEN '컴퓨터그래픽스'
        WHEN 9 THEN '웹프로그래밍'
        WHEN 10 THEN '모바일프로그래밍'
        WHEN 11 THEN '시스템분석'
        WHEN 12 THEN '정보보안'
        WHEN 13 THEN '클라우드컴퓨팅'
        WHEN 14 THEN '빅데이터'
    END || ' ' || (i % 5 + 1),
    CASE (i % 4)
        WHEN 0 THEN 1
        WHEN 1 THEN 2
        WHEN 2 THEN 3
        WHEN 3 THEN 4
    END,
    (i % 50) + 1,
    (i % 1000) + 1,
    CASE (i % 2)
        WHEN 0 THEN '1학기'
        WHEN 1 THEN '2학기'
    END,
    2020 + (i % 5)
FROM generate_series(1, 2000) i;

-- 수강신청 데이터 (100000건)
INSERT INTO school.enrollments (student_id, course_id, grade, attendance_score, midterm_score, final_score, total_score)
SELECT 
    (i % 30000) + 1,
    (i % 2000) + 1,
    CASE (i % 10)
        WHEN 0 THEN 'A+'
        WHEN 1 THEN 'A'
        WHEN 2 THEN 'B+'
        WHEN 3 THEN 'B'
        WHEN 4 THEN 'C+'
        WHEN 5 THEN 'C'
        WHEN 6 THEN 'D+'
        WHEN 7 THEN 'D'
        WHEN 8 THEN 'F'
        WHEN 9 THEN NULL
    END,
    70 + (i % 31),
    60 + (i % 41),
    65 + (i % 36),
    (70 + (i % 31)) + (60 + (i % 41)) + (65 + (i % 36))
FROM generate_series(1, 100000) i
ON CONFLICT (student_id, course_id) DO NOTHING;

-- === 직장 더미 데이터 생성 ===
-- 부서 데이터 (30개)
INSERT INTO office.departments (department_name, location, budget)
SELECT 
    CASE (i % 15)
        WHEN 0 THEN '개발팀'
        WHEN 1 THEN '기획팀'
        WHEN 2 THEN '마케팅팀'
        WHEN 3 THEN '영업팀'
        WHEN 4 THEN '인사팀'
        WHEN 5 THEN '재무팀'
        WHEN 6 THEN '총무팀'
        WHEN 7 THEN '법무팀'
        WHEN 8 THEN '디자인팀'
        WHEN 9 THEN 'QA팀'
        WHEN 10 THEN '운영팀'
        WHEN 11 THEN '고객지원팀'
        WHEN 12 THEN '연구개발팀'
        WHEN 13 THEN '품질관리팀'
        WHEN 14 THEN '전략기획팀'
    END || ' ' || ((i / 15) + 1) || '부',
    '본사 ' || (i % 10 + 1) || '층',
    (1000000 + i * 50000)::decimal
FROM generate_series(1, 30) i;

-- 직원 데이터 (30000명)
INSERT INTO office.employees (employee_number, name, department_id, position, email, phone, salary, hire_date, status)
SELECT 
    'E' || LPAD(i::text, 6, '0'),
    CASE (i % 25)
        WHEN 0 THEN '김직원'
        WHEN 1 THEN '이직원'
        WHEN 2 THEN '박직원'
        WHEN 3 THEN '정직원'
        WHEN 4 THEN '최직원'
        WHEN 5 THEN '조직원'
        WHEN 6 THEN '윤직원'
        WHEN 7 THEN '장직원'
        WHEN 8 THEN '임직원'
        WHEN 9 THEN '한직원'
        WHEN 10 THEN '오직원'
        WHEN 11 THEN '서직원'
        WHEN 12 THEN '신직원'
        WHEN 13 THEN '권직원'
        WHEN 14 THEN '황직원'
        WHEN 15 THEN '안직원'
        WHEN 16 THEN '송직원'
        WHEN 17 THEN '전직원'
        WHEN 18 THEN '홍직원'
        WHEN 19 THEN '고직원'
        WHEN 20 THEN '문직원'
        WHEN 21 THEN '양직원'
        WHEN 22 THEN '배직원'
        WHEN 23 THEN '백직원'
        WHEN 24 THEN '허직원'
    END || i,
    (i % 30) + 1,
    CASE (i % 8)
        WHEN 0 THEN '사원'
        WHEN 1 THEN '주임'
        WHEN 2 THEN '대리'
        WHEN 3 THEN '과장'
        WHEN 4 THEN '차장'
        WHEN 5 THEN '부장'
        WHEN 6 THEN '이사'
        WHEN 7 THEN '상무'
    END,
    'emp' || i || '@company.com',
    '010-' || LPAD((1000 + i % 9000)::text, 4, '0') || '-' || LPAD((i % 10000)::text, 4, '0'),
    (3000 + (i % 5000) * 10)::decimal * 10000,
    CURRENT_DATE - (i % 3650)::integer,
    CASE (i % 20)
        WHEN 0 THEN '퇴사'
        WHEN 1 THEN '휴직'
        ELSE '재직'
    END
FROM generate_series(1, 30000) i;

-- 프로젝트 데이터 (1000개)
INSERT INTO office.projects (project_name, description, start_date, end_date, budget, status, manager_id)
SELECT 
    CASE (i % 10)
        WHEN 0 THEN '웹사이트 리뉴얼'
        WHEN 1 THEN '모바일 앱 개발'
        WHEN 2 THEN 'ERP 시스템 구축'
        WHEN 3 THEN 'AI 챗봇 개발'
        WHEN 4 THEN '빅데이터 분석'
        WHEN 5 THEN '클라우드 마이그레이션'
        WHEN 6 THEN '보안 시스템 강화'
        WHEN 7 THEN 'IoT 솔루션 개발'
        WHEN 8 THEN '블록체인 플랫폼'
        WHEN 9 THEN 'VR/AR 콘텐츠'
    END || ' ' || i || '차',
    '프로젝트 ' || i || '에 대한 상세 설명입니다.',
    CURRENT_DATE - (i % 365)::integer,
    CURRENT_DATE + (30 + i % 365)::integer,
    (500000 + i * 10000)::decimal,
    CASE (i % 4)
        WHEN 0 THEN '계획'
        WHEN 1 THEN '진행중'
        WHEN 2 THEN '완료'
        WHEN 3 THEN '보류'
    END,
    (i % 30000) + 1
FROM generate_series(1, 1000) i;

-- 프로젝트 참여 데이터 (50000건)
INSERT INTO office.project_assignments (project_id, employee_id, role, hours_per_week)
SELECT 
    (i % 1000) + 1,
    (i % 30000) + 1,
    CASE (i % 6)
        WHEN 0 THEN '프로젝트 매니저'
        WHEN 1 THEN '개발자'
        WHEN 2 THEN '디자이너'
        WHEN 3 THEN 'QA 엔지니어'
        WHEN 4 THEN '분석가'
        WHEN 5 THEN '테스터'
    END,
    20 + (i % 21)
FROM generate_series(1, 50000) i
ON CONFLICT (project_id, employee_id) DO NOTHING;

-- 급여 데이터 (120000건 - 30000명 * 4개월)
INSERT INTO office.salaries (employee_id, base_salary, bonus, overtime_pay, total_amount, pay_date)
SELECT 
    ((i - 1) % 30000) + 1,
    (3000 + (((i - 1) % 30000) % 5000) * 10)::decimal * 10000,
    CASE ((i - 1) % 4)
        WHEN 3 THEN (100 + (((i - 1) % 30000) % 500))::decimal * 1000
        ELSE 0
    END,
    (50 + (i % 100))::decimal * 1000,
    (3000 + (((i - 1) % 30000) % 5000) * 10)::decimal * 10000 + 
    CASE ((i - 1) % 4)
        WHEN 3 THEN (100 + (((i - 1) % 30000) % 500))::decimal * 1000
        ELSE 0
    END + 
    (50 + (i % 100))::decimal * 1000,
    CURRENT_DATE - ((3 - ((i - 1) / 30000)) * 30)::integer
FROM generate_series(1, 120000) i;

-- 부서 manager_id 업데이트
UPDATE office.departments 
SET manager_id = (
    SELECT employee_id 
    FROM office.employees 
    WHERE department_id = office.departments.department_id 
    AND position IN ('부장', '이사', '상무')
    LIMIT 1
);

-- 인덱스 생성 (성능 향상)
-- 학교 스키마 인덱스
CREATE INDEX idx_students_department ON school.students(department_id);
CREATE INDEX idx_students_name ON school.students(name);
CREATE INDEX idx_professors_department ON school.professors(department_id);
CREATE INDEX idx_courses_department ON school.courses(department_id);
CREATE INDEX idx_courses_professor ON school.courses(professor_id);
CREATE INDEX idx_enrollments_student ON school.enrollments(student_id);
CREATE INDEX idx_enrollments_course ON school.enrollments(course_id);

-- 직장 스키마 인덱스
CREATE INDEX idx_employees_department ON office.employees(department_id);
CREATE INDEX idx_employees_name ON office.employees(name);
CREATE INDEX idx_projects_manager ON office.projects(manager_id);
CREATE INDEX idx_assignments_project ON office.project_assignments(project_id);
CREATE INDEX idx_assignments_employee ON office.project_assignments(employee_id);
CREATE INDEX idx_salaries_employee ON office.salaries(employee_id);
CREATE INDEX idx_salaries_date ON office.salaries(pay_date);

-- 생성된 데이터 통계
SELECT 'school.departments' as table_name, COUNT(*) as row_count FROM school.departments
UNION ALL
SELECT 'school.students', COUNT(*) FROM school.students
UNION ALL
SELECT 'school.professors', COUNT(*) FROM school.professors
UNION ALL
SELECT 'school.courses', COUNT(*) FROM school.courses
UNION ALL
SELECT 'school.enrollments', COUNT(*) FROM school.enrollments
UNION ALL
SELECT 'office.departments', COUNT(*) FROM office.departments
UNION ALL
SELECT 'office.employees', COUNT(*) FROM office.employees
UNION ALL
SELECT 'office.projects', COUNT(*) FROM office.projects
UNION ALL
SELECT 'office.project_assignments', COUNT(*) FROM office.project_assignments
UNION ALL
SELECT 'office.salaries', COUNT(*) FROM office.salaries; 