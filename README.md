# 🔍 쿼리 기반 DB 요금 시뮬레이터

PostgreSQL 쿼리의 실행계획을 분석하여 리소스 사용량 기반 비용을 추정하는 시스템입니다.

## 📋 프로젝트 개요

CDB 사용자는 쿼리 단위로 리소스를 소비하지만, 과금은 리소스 단가로만 추산됩니다. 이 프로젝트는 PostgreSQL 쿼리 실행계획을 분석하여 쿼리 실행에 따른 리소스 소비량을 기반으로 실제 요금을 측정합니다.

## 🛠 기술 스택

| 구분 | 기술 |
|------|------|
| Backend | Java 17 + Spring Boot 3.2.0 |
| DBMS | PostgreSQL 15 |
| DB Driver | PostgreSQL JDBC Driver |
| 실행계획 추출 | `EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON)` |
| JSON 파싱 | Jackson |
| Web UI | Thymeleaf |
| 빌드 도구 | Maven 3.6.3 |

## 🏗 시스템 아키텍처

```
User Input (SQL Query)
    ↓
ExecutionPlanAnalyzer
    ↓ EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON)
PostgreSQL 15
    ↓ JSON Response
ResourceUsageExtractor
    ↓ Resource Metrics
CostEstimator
    ↓ Cost Calculation
ResultRenderer (Web UI)
```

## 💰 비용 모델

| 리소스 항목 | 단가 (원) |
|-------------|-----------|
| CPU 1초 | ₩0.005 |
| 디스크 읽기 1MB | ₩0.0002 |
| 디스크 쓰기 1MB | ₩0.0003 |
| 정렬/해시 연산 1회 | ₩0.002 |
| 행 처리 1개 | ₩0.00001 |

## 🚀 설치 및 실행

### 1. 사전 요구사항

- Java 17
- Maven 3.6+
- PostgreSQL 15
- Git

### 2. 프로젝트 클론

```bash
git clone https://github.com/yejinj/db-resource-simulator.git
cd db-resource-simulator
```

### 3. PostgreSQL 설정

현재 서버에는 이미 PostgreSQL 15가 설치되어 있습니다:

```bash
# PostgreSQL 상태 확인
sudo systemctl status postgresql

# PostgreSQL 접속 테스트
sudo -u postgres psql -c "SELECT version();"
```

### 4. 애플리케이션 설정

`src/main/resources/application.yml` 파일에서 데이터베이스 연결 정보를 확인하고 필요시 수정하세요:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/postgres
    username: postgres
    password: password
```

### 5. 빌드 및 실행

```bash
# 의존성 설치 및 빌드
mvn clean install

# 애플리케이션 실행
mvn spring-boot:run
```

### 6. 웹 브라우저 접속

```
http://localhost:8080
```

## 📖 사용법

### 1. 웹 UI 사용

1. 브라우저에서 `http://localhost:8080` 접속
2. SQL 쿼리 입력
3. 월간 실행 횟수 입력 (선택사항)
4. "분석 시작" 버튼 클릭
5. 결과 확인

### 2. REST API 사용

#### 쿼리 분석 API

```bash
curl -X POST http://localhost:8080/api/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "sqlQuery": "SELECT * FROM users WHERE age > 25;",
    "executionsPerMonth": 10000
  }'
```

#### 헬스체크 API

```bash
curl http://localhost:8080/api/health
```

## 📊 예시 결과

```json
{
  "success": true,
  "costResult": {
    "totalCost": 5.024,
    "costBreakdown": {
      "CPU 비용": 3.412,
      "디스크 읽기 비용": 1.123,
      "디스크 쓰기 비용": 0.234,
      "정렬/해시 비용": 0.002,
      "행 처리 비용": 0.253
    }
  }
}
```

## 🎯 주요 기능

- ✅ PostgreSQL 실행계획 자동 분석
- ✅ 리소스 사용량 기반 비용 계산
- ✅ 월간 예상 비용 추정
- ✅ 웹 UI 제공
- ✅ REST API 제공
- ✅ 샘플 쿼리 제공

## 🔧 개발 환경

### 서버 사양
- **OS**: Ubuntu 20.04.3 LTS
- **메모리**: 64GB (PostgreSQL 최적화 완료)
- **CPU**: Intel Xeon Gold 5220 (32코어)
- **PostgreSQL**: 15.13 (64GB 메모리 최적화 설정)

### PostgreSQL 최적화 설정
- `shared_buffers`: 16GB (메모리의 25%)
- `effective_cache_size`: 48GB (메모리의 75%)
- `work_mem`: 64MB
- `maintenance_work_mem`: 2GB
- `max_connections`: 200

## 📝 테스트 시나리오

### 1. 전체 테이블 스캔
```sql
SELECT * FROM users WHERE age > 25;
```
→ Full Seq Scan 발생, 디스크 읽기 집중

### 2. 인덱스 스캔
```sql
SELECT * FROM users WHERE id = 1;
```
→ Index Scan 사용, CPU 비용 최소화

### 3. 집계 쿼리
```sql
SELECT COUNT(*) FROM orders WHERE status = 'CANCELLED';
```
→ Index Scan + Aggregate, CPU + 정렬 비용

### 4. 조인 쿼리
```sql
SELECT u.name, o.amount 
FROM users u JOIN orders o ON u.id = o.user_id 
WHERE o.amount > 5000;
```
→ Hash Join 또는 Merge Join, 메모리 사용량 증가

## 🤝 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feat/amazing-feature`)
3. Commit your Changes (`git commit -m 'feat: add amazing feature'`)
4. Push to the Branch (`git push origin feat/amazing-feature`)
5. Open a Pull Request

## 📄 라이선스

이 프로젝트는 MIT 라이선스를 따릅니다.

## 👥 개발자

- **yejinj** - *Initial work* - [GitHub](https://github.com/yejinj)

## 📞 문의

프로젝트에 대한 문의사항이 있으시면 이슈를 등록해주세요. 