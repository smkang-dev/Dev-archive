# Dev Archive

개발 공부 중 다시 참고하고 싶은 문서와 링크를 카테고리별로 저장하고 관리하는 웹 애플리케이션입니다.

자료의 제목, URL, 메모를 함께 기록할 수 있으며, 통합 검색과 드래그 앤 드롭을 이용한 카테고리·자료 순서 변경 기능을 구현했습니다. 변경한 배치와 순서는 데이터베이스에 저장되어 애플리케이션을 다시 실행한 뒤에도 유지됩니다.

---

## 주요 기능

- 카테고리 생성 및 삭제
- 개발 자료 등록, 수정, 삭제
- 제목, URL, 메모 통합 검색
- 카테고리 순서 변경
- 같은 카테고리 내 자료 순서 변경
- 드래그 앤 드롭을 이용한 자료의 카테고리 이동
- 카테고리별 DB 페이지 조회: 페이지당 4개
- 화면 버전 확인을 통한 오래된 수정 요청 차단
- MySQL을 이용한 데이터 영구 저장

---

## 핵심 구현 내용

### 카테고리별 자료 관리

- 자료의 제목, URL, 메모를 카테고리와 함께 저장
- 카테고리별로 자료를 묶어 카드 형태로 표시
- 일반 조회와 검색에서 카테고리별로 DB에서 최대 4개씩 조회하고 카드별 페이지 이동 지원
- 입력값의 공백, 길이, URL 형식을 Service 계층에서 검증

### 통합 검색

- 제목, URL, 메모를 대상으로 대소문자 구분 없이 검색
- JPQL의 부분 일치 검색을 이용하여 하나의 키워드로 관련 자료 조회
- 검색 결과 수 표시 및 결과가 없는 경우 별도 안내 화면 제공
- `%`, `_`, `!`를 이스케이프하여 검색어를 문자 그대로 처리
- 검색과 일반 조회에서는 드래그를 비활성화하고, 전체 목록을 사용하는 별도 순서 편집 모드 제공

### 드래그 앤 드롭 정렬

- 상단 `순서 편집`으로 진입한 뒤 JavaScript Drag and Drop API로 카테고리·자료 순서 변경
- 자료를 다른 카테고리로 이동하는 기능 구현
- 변경된 전체 배치를 JSON으로 서버에 전송하고 `sortOrder`를 데이터베이스에 저장
- 서버에서 카테고리·자료 ID의 누락, 중복, 알 수 없는 ID, null, 중복 그룹을 모두 검증한 뒤 하나의 트랜잭션으로 반영
- 기본 `기타` 카테고리는 마지막 위치에 고정
- 전체 배치를 전송하는 방식이므로 순서 편집은 자료 500개 이하에서 지원

### 동시 수정과 화면 버전 관리

- `archive_state` 관리 행에 전체 자료의 `revision` 저장
- 자료·카테고리 생성, 수정, 삭제 및 정렬 시 동일 관리 행에 `PESSIMISTIC_WRITE` 잠금 획득
- 화면에서 전달한 `expectedRevision`과 현재 버전을 비교하고 일치하는 경우에만 변경 수행
- 오래된 화면에서 수정하면 HTTP 409로 거부하여 다른 탭의 최신 변경을 덮어쓰는 문제 방지
- 잘못된 입력·정렬 정보는 HTTP 400으로 처리하고, 일반 폼에는 오류 화면, 정렬 요청에는 JSON 메시지 반환
- 조회는 읽기 전용 `REPEATABLE_READ` 트랜잭션에서 목록과 revision을 함께 읽도록 구성

모든 쓰기를 하나의 관리 행으로 직렬화하는 개인 자료 저장소용 설계입니다. 다중 사용자 서비스로 확장할 경우 사용자별 데이터와 잠금 범위를 분리해야 합니다.

### 안전한 카테고리 삭제

- 최초 초기화 시 기본 `기타` 카테고리를 준비하고 기존 표시 순서를 정규화
- `ApplicationRunner`에서 트랜잭션 서비스를 호출하고, 완료 여부를 저장해 이후 재시작에서는 초기화 생략
- 사용자 카테고리 삭제 시 내부 자료를 함께 삭제하지 않고 `기타` 카테고리로 이동
- 기본 카테고리 삭제를 제한하여 카테고리에 속하지 않는 자료가 발생하지 않도록 처리
- 자료 이동 또는 삭제 후 `sortOrder`를 다시 정규화하여 연속된 표시 순서 유지

### 계층형 구조와 트랜잭션

- Controller - Service - Repository - Domain 계층으로 역할 분리
- 조회 작업에는 읽기 전용 트랜잭션을 적용하고, 생성·수정·삭제·정렬 작업에는 쓰기 트랜잭션 적용
- Spring Data JPA를 이용하여 엔티티의 변경 감지와 연관관계 관리
- `@PrePersist`, `@PreUpdate`를 이용하여 자료의 생성·수정 시각 자동 기록

---

## 기술 스택

- Java 21
- Spring Boot
- Spring MVC
- Spring Data JPA
- Thymeleaf
- MySQL
- HTML / CSS / JavaScript
- Gradle
- JUnit / H2: 테스트용 메모리 DB
- Git / GitHub

---

## 프로젝트 구조

```text
├─ src
│  ├─ main
│  │  ├─ java/com/devarchive
│  │  │  ├─ controller
│  │  │  ├─ domain
│  │  │  ├─ repository
│  │  │  ├─ service
│  │  │  └─ DevArchiveApplication.java
│  │  └─ resources
│  │     ├─ static/css/style.css
│  │     ├─ templates
│  │     │  ├─ index.html
│  │     │  └─ request-error.html
│  │     └─ application.properties
│  └─ test
│     ├─ java/com/devarchive/DevArchiveApplicationTests.java
│     └─ resources
│        ├─ application.properties
│        └─ test-data.sql
├─ 001_archive_state.sql
├─ build.gradle
└─ settings.gradle
```

---

## 데이터베이스 구성

### categories

- id
- name
- default_category
- sort_order

### archives

- id
- category_id
- title
- url
- memo
- created_at
- updated_at
- sort_order

`archives.category_id`를 통해 각 자료가 하나의 카테고리에 속하도록 구성했습니다.

### archive_state

- id: 관리 행 식별자, 현재 1 사용
- revision: 성공한 변경마다 증가하는 화면 버전
- initialized: 최초 데이터 초기화 완료 여부

---

## 실행 방법

### 요구 환경

- Java 21
- MySQL

### 1. 데이터베이스 생성

MySQL에서 프로젝트가 사용할 데이터베이스를 생성합니다.

```sql
CREATE DATABASE dev_archive;
```

### 2. 데이터베이스 연결 설정

민감한 접속 정보는 Git에서 제외된 `src/main/resources/application-secret.properties` 파일에 작성합니다.

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/dev_archive
spring.datasource.username=root
spring.datasource.password=비밀번호
spring.jpa.hibernate.ddl-auto=update
```

> 실제 데이터베이스 비밀번호가 포함된 `application-secret.properties` 파일은 GitHub에 커밋하지 않도록 주의하세요.

### 3. 관리 테이블 준비

MySQL Workbench에서 앱이 사용하는 DB를 선택하고, **프로젝트 루트의 `001_archive_state.sql`**을 실행합니다.

```sql
USE dev_archive;
```

스크립트는 `archive_state` 테이블과 id=1 행을 준비합니다. 재실행해도 기존 revision과 초기화 상태를 0으로 되돌리지 않습니다. `categories`와 `archives`는 위 `ddl-auto=update` 설정에 따라 애플리케이션 실행 시 생성·갱신됩니다.

### 4. 애플리케이션 실행

Windows에서는 다음 명령어를 실행합니다.

```powershell
.\gradlew.bat bootRun
```

macOS 또는 Linux에서는 다음 명령어를 실행합니다.

```bash
./gradlew bootRun
```

실행 후 브라우저에서 `http://localhost:8080`으로 접속합니다.

---

## 테스트 및 검증

프로젝트 루트에서 다음 명령으로 테스트를 실행합니다.

Windows PowerShell:

```powershell
.\gradlew.bat clean test
```

macOS / Linux:

```bash
./gradlew clean test
```

- `src/test/resources/application.properties`의 H2 메모리 DB를 사용하며 실제 MySQL 연결 설정은 사용하지 않음
- 초기화, 잘못된 정렬 요청, 카테고리 이동·삭제, 페이지 조회·검색, 오류 응답, 화면 렌더링 등 13개 테스트 포함
- 같은 revision으로 동시에 변경할 때 하나만 성공하는 시나리오 검증
- 테스트 보고서: `build/reports/tests/test/index.html`

H2 테스트와 실제 MySQL 검증은 구분합니다. MySQL에서는 두 탭을 연 뒤 한 탭에서 저장하고, 새로고침하지 않은 다른 탭의 변경이 거부되는지 확인할 수 있습니다. 대량 데이터 성능 수치는 아직 측정하지 않았습니다.

---

## 프로젝트에서 배운 점

- Spring MVC와 Thymeleaf를 이용하여 서버에서 조회한 데이터를 동적 화면으로 구성하는 방법을 익혔습니다.
- Spring Data JPA의 연관관계와 변경 감지를 활용하여 카테고리 이동 및 정렬 순서를 관리했습니다.
- 드래그 앤 드롭으로 변경된 화면 상태를 JSON 요청으로 전달하고, 서버에서 데이터 정합성을 검증한 뒤 저장하는 과정을 구현했습니다.
- 카테고리 삭제 시 자료를 기본 카테고리로 이동하도록 설계하면서 삭제 기능에서도 기존 데이터를 안전하게 보존하는 방법을 고민했습니다.
- DB 잠금과 화면 버전 검사를 함께 적용해 동시 쓰기 직렬화와 오래된 화면의 덮어쓰기 방지를 구분했습니다.
- 페이지 조회와 전체 배치 정렬의 데이터 범위가 다르다는 점을 고려해 조회 모드와 순서 편집 모드를 분리했습니다.

---

## 향후 개선 사항

- Spring Security를 이용한 로그인 및 사용자별 저장 공간 분리
- 태그와 즐겨찾기를 이용한 자료 분류 기능
- 검색 조건 및 정렬 방식 확장
- 순서 편집을 전체 배치 전송에서 부분 이동 API로 개선하여 500개 제한 완화
- 실제 MySQL 환경의 동시성 통합 테스트와 CI 자동 실행 보강
- 카테고리 수 증가에 따른 조회·count 쿼리 비용 및 부분 일치 검색 성능 측정
- 클라우드 환경 배포

