# Chat Web App

실시간 공개 채팅/개인 채팅을 제공하는 **Spring Boot 기반 마이크로서비스 애플리케이션**입니다.
서비스를 기능 단위로 분리하고(API Gateway, 인증, 채팅, 프로필, 서비스 디스커버리), Docker/Kubernetes 배포를 고려한 구조로 구성되어 있습니다.

---

## 1) 프로젝트 개요

이 프로젝트는 다음 목표를 중심으로 설계되었습니다.

- 실시간 메시징(공개 채팅 + 1:1 개인 채팅)
- OAuth2 기반 로그인 및 인증 정보 전파(JWT/쿠키 기반)
- 서비스 분리로 확장성과 유지보수성 확보
- 컨테이너 기반 로컬/클라우드 배포 용이성 확보

---

## 2) 서비스 구성

- **api-gateway**
  외부 요청의 단일 진입점 역할을 수행하며 내부 마이크로서비스로 라우팅합니다.

- **oauth**
  OAuth2 로그인(예: Google)과 인증/인가 처리, 사용자 기본 정보 관리를 담당합니다.

- **chat-service**
  공개 채팅/개인 채팅 메시지 처리, 대화방(Conversation) 및 메시지 저장 로직을 담당합니다.

- **profile-service**
  사용자 프로필(표시 이름, 프로필 이미지 등) 관련 조회/수정 기능을 담당합니다.

- **eureka-server**
  서비스 등록/탐색(Service Discovery)을 담당하며, 동적으로 서비스 위치를 찾을 수 있게 합니다.

---

## 3) 기술 스택 (상세)

### 백엔드

- **Java 17**
  각 서비스의 주요 구현 언어입니다.

- **Spring Boot 3.x**
  서비스별 독립 실행 애플리케이션 구성, 내장 WAS 기반 실행, 환경설정 단순화에 사용됩니다.

- **Spring Web / MVC**
  REST API 및 View(Thymeleaf) 렌더링 엔드포인트 구현에 사용됩니다.

- **Spring Security**
  인증/인가, 보안 필터 체인, 로그인/로그아웃 처리 등에 사용됩니다.

- **Spring Security OAuth2 Client**
  Google OAuth2 로그인 연동을 처리합니다.

- **Spring Cloud Gateway**
  API Gateway에서 요청 라우팅, 경로 기반 분기, 서비스 연계를 담당합니다.

- **Spring Cloud Netflix Eureka**
  서비스 등록/탐색을 통해 마이크로서비스 간 호출 유연성을 높입니다.

- **Spring Data MongoDB**
  채팅 메시지/대화 데이터 등 문서 지향 데이터 처리를 담당합니다.

- **WebSocket (STOMP)**
  채팅 실시간 양방향 통신을 구현합니다.

- **WebClient**
  서비스 간 비동기 HTTP 호출(예: 프로필/유저 정보 조회)에 사용됩니다.

### 데이터/인증

- **MongoDB**
  메시지, 대화, 사용자/프로필 관련 문서 저장소로 사용됩니다.

- **JWT(JSON Web Token)**
  서비스 간 인증 컨텍스트 전달에 사용되며, 쿠키 기반 인증 흐름과 결합되어 사용됩니다.

- **OAuth2 (Google)**
  외부 소셜 로그인 공급자를 통한 사용자 인증에 사용됩니다.

### 프론트엔드

- **Thymeleaf**
  서버 사이드 렌더링 템플릿 엔진으로 페이지 생성에 사용됩니다.

- **HTML / CSS / JavaScript**
  UI 마크업, 스타일링, 클라이언트 동작 구현에 사용됩니다.

- **Bootstrap**
  반응형 UI 구성과 공통 스타일링을 빠르게 적용하기 위해 사용됩니다.

### 인프라/배포

- **Docker**
  각 서비스를 컨테이너 이미지로 패키징하고 실행 환경 일관성을 보장합니다.

- **Docker Compose**
  로컬 멀티 컨테이너 개발 환경(서비스 + 데이터스토어) 실행에 사용됩니다.

- **Kubernetes**
  서비스 오케스트레이션/확장/배포 관리를 위한 매니페스트 기반 운영에 사용됩니다.

### 빌드/도구

- **Maven**
  멀티 서비스 빌드 및 의존성 관리를 담당합니다.

- **Maven Wrapper (`mvnw`)**
  로컬 Maven 설치 여부와 무관하게 동일 버전으로 빌드할 수 있도록 지원합니다.

---

## 4) 디렉터리 구조

```text
.
├── api-gateway/
├── chat-service/
├── eureka-server/
├── oauth/
├── profile-service/
├── docker-compose.yml
└── kubernetes.yml
```

---

## 5) 실행 방법 (로컬)

### 방법 A. 서비스별 실행

각 서비스 디렉터리에서:

```bash
./mvnw spring-boot:run
```

권장 실행 순서:
1. eureka-server
2. oauth
3. profile-service
4. chat-service
5. api-gateway

### 방법 B. Docker Compose 실행

```bash
docker compose up --build
```

---

## 6) Private 저장소로 복제(본인 GitHub Repo로 이전) 방법

현재 실행 환경에서는 사용자의 GitHub 계정 인증 토큰/권한에 직접 접근할 수 없어, 원격 저장소 생성/푸시는 사용자가 한 번만 실행해야 합니다. 아래 절차대로 진행하면 됩니다.

1. GitHub에서 **새 Private Repository** 생성 (예: `chat-web-app-private`)
2. 로컬에서 원격 추가

```bash
git remote add my-private-repo https://github.com/<YOUR_ID>/chat-web-app-private.git
```

3. 브랜치 푸시

```bash
git push -u my-private-repo <YOUR_BRANCH>
```

4. 기본 브랜치 푸시(예: main)

```bash
git push my-private-repo main
```

> SSH를 사용하는 경우 remote URL을 `git@github.com:<YOUR_ID>/chat-web-app-private.git` 형태로 바꾸면 됩니다.

---

## 7) 향후 개선 아이디어

- React/Vue 기반 SPA 프론트엔드 분리
- 채팅 메시지 읽음 처리, 안 읽은 메시지 카운트
- Redis Pub/Sub 기반 다중 인스턴스 WebSocket 확장
- 관측성(로그/메트릭/트레이싱) 고도화
- CI/CD 파이프라인(GitHub Actions) 구축
