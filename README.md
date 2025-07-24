# PostgreSQL 쿼리 분석기

## 개요

PostgreSQL 데이터베이스에서 SQL 쿼리의 성능을 분석하고 최적화 제안을 제공하는 웹 애플리케이션입니다.

## 주요 기능

### 1. 쿼리 성능 분석
- SQL 쿼리 실행 및 실행 계획 분석
- EXPLAIN ANALYZE 자동 실행
- 실행 시간, 비용, 처리된 행 수 분석
- 리소스 사용량 분석

### 2. 최적화 제안
- 누락된 인덱스 감지 및 제안
- 비효율적인 쿼리 패턴 식별
- 성능 개선을 위한 권장사항 제공

### 3. 스키마 브라우저
- 데이터베이스 테이블 목록 조회
- 테이블 구조 및 인덱스 정보 표시
- 데이터 미리보기 기능

## 🚀 설치 및 실행

### 사전 요구사항
- Java 17+
- Maven 3.6+
- PostgreSQL 13+

### 실행 방법
```bash
# 1. 저장소 클론
git clone <repository-url>
cd db-resource-simulator

# 2. 의존성 설치 및 빌드
mvn clean compile

# 3. 애플리케이션 실행
mvn spring-boot:run

# 4. 웹 브라우저에서 접속
# http://localhost:8090
```

### 데이터베이스 설정
application.properties 파일에서 데이터베이스 연결 정보를 설정합니다:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/your_database
spring.datasource.username=your_username
spring.datasource.password=your_password
```

## 📊 사용 방법

### 1. 쿼리 실행 및 분석
1. 메인 페이지에서 분석할 SQL 쿼리를 입력
2. "쿼리 실행" 버튼 클릭
3. 실행 결과와 성능 분석 결과 확인
4. 최적화 제안사항 검토

### 2. 스키마 탐색
1. "스키마 브라우저" 메뉴 선택
2. 테이블 목록에서 원하는 테이블 선택
3. 테이블 구조 및 데이터 확인

## 🛠️ 기술 스택

- **백엔드**: Spring Boot, Java 17
- **프론트엔드**: Thymeleaf, HTML/CSS/JavaScript
- **데이터베이스**: PostgreSQL
- **빌드 도구**: Maven

## 📝 라이선스

이 프로젝트는 MIT 라이선스를 따릅니다.
