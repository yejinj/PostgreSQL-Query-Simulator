# PostgreSQL 쿼리 분석기

PostgreSQL 쿼리의 실행계획을 분석하여 성능 메트릭을 측정하고, 데이터베이스 최적화 제안을 제공하는 웹 애플리케이션입니다.

## 주요 기능

### 쿼리 성능 분석
- **실행계획 분석**: EXPLAIN ANALYZE를 통한 상세 실행계획 시각화
- **리소스 사용량 측정**: CPU, 메모리, I/O, 네트워크 리소스 사용량 분석
- **성능 메트릭**: 실행 시간, 버퍼 히트율, 디스크 읽기/쓰기 통계
- **최적화 제안**: 인덱스, 조인 최적화, WHERE 절 개선 제안

### 스키마 브라우저
- **데이터베이스 구조 탐색**: 스키마, 테이블, 컬럼 정보 브라우징
- **테이블 데이터 뷰어**: 실제 데이터 미리보기 및 페이징
- **통계 정보**: 테이블별 레코드 수, 크기 정보 표시

## 실행 방법

### 1. 사전 준비
PostgreSQL이 설치되어 있어야 합니다.

### 2. 환경변수 설정 (선택사항)
`.env` 파일을 생성하거나 시스템 환경변수를 설정하세요:

```bash
# 데이터베이스 설정
DB_URL=jdbc:postgresql://127.0.0.1:5432/postgres
DB_USERNAME=postgres
DB_PASSWORD=postgres

# 서버 설정
SERVER_PORT=8090

# 로깅 설정
SHOW_SQL=true
LOG_LEVEL_SIMULATOR=DEBUG
```

### 3. 애플리케이션 실행
```bash
# Maven으로 실행
mvn spring-boot:run

# 또는 jar 파일로 실행
mvn package
java -jar target/db-resource-simulator-1.0.0.jar
```
