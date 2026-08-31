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
- 카테고리별 자료 페이지 분할 표시
- MySQL을 이용한 데이터 영구 저장

---

## 핵심 구현 내용

### 카테고리별 자료 관리

- 자료의 제목, URL, 메모를 카테고리와 함께 저장
- 카테고리별로 자료를 묶어 카드 형태로 표시
- 카테고리 하나에 자료가 많아져도 확인하기 쉽도록 카드별 페이지 이동 기능 구현
- 입력값의 공백, 길이, URL 형식을 Service 계층에서 검증

### 통합 검색

- 제목, URL, 메모를 대상으로 대소문자 구분 없이 검색
- JPQL의 부분 일치 검색을 이용하여 하나의 키워드로 관련 자료 조회
- 검색 결과 수 표시 및 결과가 없는 경우 별도 안내 화면 제공
- 검색 중에는 일부 자료만 화면에 존재하므로 순서 변경 기능을 비활성화하여 잘못된 정렬 정보 저장 방지

### 드래그 앤 드롭 정렬

- JavaScript Drag and Drop API를 이용하여 카테고리와 자료의 순서 변경
- 자료를 다른 카테고리로 이동하는 기능 구현
- 변경된 전체 배치를 JSON으로 서버에 전송하고 `sortOrder`를 데이터베이스에 저장
- 서버에서 카테고리 및 자료 ID의 누락과 중복 여부를 검증한 뒤 하나의 트랜잭션으로 반영

### 안전한 카테고리 삭제

- 애플리케이션 시작 시 기본 `기타` 카테고리가 항상 존재하도록 초기화
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
- Git / GitHub

---

## 프로젝트 구조

```text
src
├─ main
│  ├─ java/com/devarchive
│  │  ├─ controller
│  │  ├─ domain
│  │  ├─ repository
│  │  ├─ service
│  │  └─ DevArchiveApplication.java
│  └─ resources
│     ├─ static/css
│     │  └─ style.css
│     ├─ templates
│     │  └─ index.html
│     └─ application.properties
└─ test
   └─ java/com/devarchive
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

### 3. 애플리케이션 실행

Windows에서는 다음 명령어를 실행합니다.

```bash
gradlew.bat bootRun
```

macOS 또는 Linux에서는 다음 명령어를 실행합니다.

```bash
./gradlew bootRun
```

실행 후 브라우저에서 `http://localhost:8080`으로 접속합니다.

---

## 프로젝트에서 배운 점

- Spring MVC와 Thymeleaf를 이용하여 서버에서 조회한 데이터를 동적 화면으로 구성하는 방법을 익혔습니다.
- Spring Data JPA의 연관관계와 변경 감지를 활용하여 카테고리 이동 및 정렬 순서를 관리했습니다.
- 드래그 앤 드롭으로 변경된 화면 상태를 JSON 요청으로 전달하고, 서버에서 데이터 정합성을 검증한 뒤 저장하는 과정을 구현했습니다.
- 카테고리 삭제 시 자료를 기본 카테고리로 이동하도록 설계하면서 삭제 기능에서도 기존 데이터를 안전하게 보존하는 방법을 고민했습니다.

---

## 향후 개선 사항

- Spring Security를 이용한 로그인 및 사용자별 저장 공간 분리
- 태그와 즐겨찾기를 이용한 자료 분류 기능
- 검색 조건 및 정렬 방식 확장
- 예외 발생 시 사용자에게 검증 메시지를 표시하는 오류 처리 화면
- Service 및 Controller 계층 테스트 보강
- 클라우드 환경 배포
