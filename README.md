# Kubernetes-Docker Web-Chat

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

- **frontend (React SPA)**
  API Gateway를 단일 진입점으로 사용하는 분리형 프론트엔드입니다.

---

## 2.5) 인프라 아키텍처 구성도 (AWS 아이콘 기반 SVG)

로컬 Kubernetes(kind)와 AWS EKS(dev/prod) 운영 구조를 하나의 SVG로 정리했습니다.

![Local k8s + AWS EKS Architecture](docs/infra-architecture-local-k8s-eks.svg)

---

## 3) 서비스 시퀀스 다이어그램

아래 다이어그램은 사용자 로그인 후 공개 채팅 메시지를 송수신하는 핵심 흐름을 나타냅니다.

```mermaid
sequenceDiagram
    autonumber
    actor U as User(Browser)
    participant G as API Gateway
    participant O as Auth Service
    participant E as Eureka
    participant C as Chat Service
    participant P as Profile Service
    participant MU as Mongo Users
    participant MM as Mongo Messages

    U->>G: GET /auth/login
    G->>O: Route /auth/**
    O-->>U: Google OAuth Login Page
    U->>O: OAuth2 callback
    O->>MU: Upsert user
    O-->>U: JWT cookie + redirect /chat

    U->>G: GET /chat/public
    G->>C: Route /chat/**
    C->>E: Resolve service instances
    C-->>U: Public chat page (STOMP client)

    U->>C: SEND /app/sendMessage (WebSocket)
    C->>MM: Save message
    C-->>U: BROADCAST /topic/messages
    U->>G: GET /profile/{email}
    G->>P: Route /profile/**
    P-->>U: displayName/profilePic
```

## 4) 기술 스택 (상세)

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

- **Redis**
  다중 인스턴스 WebSocket 이벤트 릴레이(Pub/Sub)에 사용됩니다.

- **JWT(JSON Web Token)**
  서비스 간 인증 컨텍스트 전달에 사용되며, 쿠키 기반 인증 흐름과 결합되어 사용됩니다.

- **OAuth2 (Google)**
  외부 소셜 로그인 공급자를 통한 사용자 인증에 사용됩니다.

### 프론트엔드

- **React + TypeScript + Vite**
  분리형 SPA 애플리케이션 구현에 사용됩니다.

- **Thymeleaf**
  기존 서버 렌더링 화면(점진 이관 대상)에 사용됩니다.

- **Tailwind CSS**
  SPA/서버 템플릿 화면의 유틸리티 기반 스타일링에 사용됩니다.

### 인프라/배포

- **Docker**
  각 서비스를 컨테이너 이미지로 패키징하고 실행 환경 일관성을 보장합니다.

- **Docker Compose**
  로컬 멀티 컨테이너 개발 환경(서비스 + 데이터스토어) 실행에 사용됩니다.

- **Kubernetes**
  서비스 오케스트레이션/확장/배포 관리를 위한 매니페스트 기반 운영에 사용됩니다.

- **Prometheus**
  Spring Actuator + Micrometer 메트릭 수집에 사용됩니다.

### 빌드/도구

- **Maven**
  멀티 서비스 빌드 및 의존성 관리를 담당합니다.

- **Maven Wrapper (`mvnw`)**
  로컬 Maven 설치 여부와 무관하게 동일 버전으로 빌드할 수 있도록 지원합니다.

---

## 5) 디렉터리 구조

```text
.
├── api-gateway/
├── chat-service/
├── eureka-server/
├── frontend/
├── monitoring/
├── oauth/
├── profile-service/
├── docker-compose.yml
└── kubernetes.yml
```

---

## 6) 실행 방법 (로컬/클라우드)

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
6. frontend

프론트엔드 실행:

```bash
cd frontend
npm install
npm run dev
```

### 방법 B. Docker Compose 실행

```bash
docker compose up --build
```

- SPA: `http://localhost:5173`
- API Gateway: `http://localhost:9999`

### 방법 C. 테스트용 초기화 + 재실행 스크립트

기존 Docker 컨테이너/이미지를 모두 정리한 뒤, 이 레포를 다시 빌드/실행합니다.

```bash
./reset-and-run.sh
```

자동 확인(비대화형) 실행:

```bash
./reset-and-run.sh -y
```

### 방법 D. Docker 리셋 + MSA(Compose) + Kubernetes(kind) 일괄 배포

WSL/Ubuntu + Docker 환경에서 Docker 초기화부터 Compose 배포, kind 기반 Kubernetes 배포까지 한 번에 실행합니다.

```bash
./deploy-all.sh
```

비대화형 실행:

```bash
./deploy-all.sh -y
```

### 방법 E. AWS EKS 배포 (aws cli + eksctl + kubectl)

아래는 `kubernetes.yml` 기준으로 EKS에 순차 배포하는 예시입니다.
현재 매니페스트가 local kind 기준(`imagePullPolicy: Never`)이므로 EKS 적용 전 변환 단계를 포함합니다.

1) 공통 변수 설정

```sh
set -euo pipefail

export AWS_REGION="ap-northeast-2"
export CLUSTER_NAME="chat-dev-eks"
export NODEGROUP_NAME="chat-dev-ng"
export ECR_PREFIX="chat-app"
export AWS_ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
export ECR_REGISTRY="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
```

2) AWS CLI 인증/권한 확인

```sh
aws configure
aws sts get-caller-identity
aws ec2 describe-availability-zones --region "$AWS_REGION" --query 'AvailabilityZones[].ZoneName' --output text
```

3) EKS 클러스터 생성 (`eksctl`)

```sh
eksctl create cluster \
  --name "$CLUSTER_NAME" \
  --region "$AWS_REGION" \
  --version 1.30 \
  --nodegroup-name "$NODEGROUP_NAME" \
  --node-type t3.large \
  --nodes 3 \
  --nodes-min 2 \
  --nodes-max 4 \
  --managed
```

4) kubeconfig 연결 및 노드 확인 (`aws cli` + `kubectl`)

```sh
aws eks update-kubeconfig --region "$AWS_REGION" --name "$CLUSTER_NAME"
kubectl config current-context
kubectl get nodes -o wide
```

5) ECR 리포지토리 생성 및 로그인 (`aws cli`)

```sh
aws ecr get-login-password --region "$AWS_REGION" \
| docker login --username AWS --password-stdin "$ECR_REGISTRY"

for repo in eureka-server api-gateway frontend auth-service chat-service profile-service; do
  aws ecr describe-repositories --region "$AWS_REGION" --repository-names "${ECR_PREFIX}/${repo}" >/dev/null 2>&1 \
    || aws ecr create-repository --region "$AWS_REGION" --repository-name "${ECR_PREFIX}/${repo}"
done
```

6) 서비스 이미지 빌드/푸시

```sh
docker build -t "$ECR_REGISTRY/$ECR_PREFIX/eureka-server:latest" ./eureka-server
docker build -t "$ECR_REGISTRY/$ECR_PREFIX/api-gateway:latest" ./api-gateway
docker build -t "$ECR_REGISTRY/$ECR_PREFIX/frontend:latest" ./frontend
docker build -t "$ECR_REGISTRY/$ECR_PREFIX/auth-service:latest" ./oauth
docker build -t "$ECR_REGISTRY/$ECR_PREFIX/chat-service:latest" ./chat-service
docker build -t "$ECR_REGISTRY/$ECR_PREFIX/profile-service:latest" ./profile-service

docker push "$ECR_REGISTRY/$ECR_PREFIX/eureka-server:latest"
docker push "$ECR_REGISTRY/$ECR_PREFIX/api-gateway:latest"
docker push "$ECR_REGISTRY/$ECR_PREFIX/frontend:latest"
docker push "$ECR_REGISTRY/$ECR_PREFIX/auth-service:latest"
docker push "$ECR_REGISTRY/$ECR_PREFIX/chat-service:latest"
docker push "$ECR_REGISTRY/$ECR_PREFIX/profile-service:latest"
```

7) EKS용 매니페스트 생성 (`kubectl` 적용 전 변환)

```sh
sed \
  -e "s|chat-app-eureka-server:latest|$ECR_REGISTRY/$ECR_PREFIX/eureka-server:latest|g" \
  -e "s|chat-app-api-gateway:latest|$ECR_REGISTRY/$ECR_PREFIX/api-gateway:latest|g" \
  -e "s|chat-app-frontend:latest|$ECR_REGISTRY/$ECR_PREFIX/frontend:latest|g" \
  -e "s|chat-app-auth-service:latest|$ECR_REGISTRY/$ECR_PREFIX/auth-service:latest|g" \
  -e "s|chat-app-chat-service:latest|$ECR_REGISTRY/$ECR_PREFIX/chat-service:latest|g" \
  -e "s|chat-app-profile-service:latest|$ECR_REGISTRY/$ECR_PREFIX/profile-service:latest|g" \
  -e "s|imagePullPolicy: Never|imagePullPolicy: IfNotPresent|g" \
  kubernetes.yml > kubernetes-eks.yml
```

8) 배포 및 롤아웃 확인 (`kubectl`)

```sh
kubectl apply -f kubernetes-eks.yml
kubectl -n chat-app rollout status deployment/eureka-server
kubectl -n chat-app rollout status deployment/api-gateway
kubectl -n chat-app rollout status deployment/auth-service
kubectl -n chat-app rollout status deployment/chat-service
kubectl -n chat-app rollout status deployment/profile-service
kubectl -n chat-app rollout status deployment/frontend
kubectl -n chat-app get pods,svc
```

9) 외부 접근 테스트 (임시: frontend Service를 LoadBalancer로 패치)

```sh
kubectl -n chat-app patch svc frontend -p '{"spec":{"type":"LoadBalancer"}}'
kubectl -n chat-app get svc frontend -w
```

10) 정리(필요 시)

```sh
eksctl delete cluster --name "$CLUSTER_NAME" --region "$AWS_REGION"
```

### OAuth 테스트 유저 자동 시드

`auth-service` 시작 시 아래 테스트 유저를 MongoDB(`chat-app-users.users`)에 upsert 합니다.

- `test1` / `123456`
- `test2` / `123456`
- `test3` / `123456`

---

## 7) UI 화면 캡처

아래 이미지는 `mcr.microsoft.com/playwright:v1.51.1-jammy` Docker 이미지로 자동 캡처했습니다.

```bash
docker run --rm \
  --add-host host.docker.internal:host-gateway \
  -v "$PWD:/work" -w /work \
  mcr.microsoft.com/playwright:v1.51.1-jammy \
  bash -lc "mkdir -p /tmp/pw && npm --prefix /tmp/pw init -y >/dev/null 2>&1 && npm --prefix /tmp/pw install playwright@1.51.1 >/dev/null 2>&1 && NODE_PATH=/tmp/pw/node_modules node scripts/capture-screenshots.cjs"
```

### 실행 후 화면

![실행 후 로그인 페이지](docs/screenshots/01-app-running-login-page.png)

### 로그인 후 화면

![로그인 후 홈 화면](docs/screenshots/02-after-login-home-page.png)

### 로그인 후 채팅 화면

![로그인 후 채팅 화면](docs/screenshots/03-after-login-chat-page.png)

## 8) Private 저장소로 복제(본인 GitHub Repo로 이전) 방법

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

## 9) 9.1~9.5 개발 진행 상태

### 9.1 SPA 프론트엔드 분리
- `frontend/` (React + TypeScript + Vite + Tailwind) 신규 추가
- 로그인 후 대시보드/공개채팅/개인채팅 페이지 분리
- Nginx 기반 컨테이너(`frontend/Dockerfile`)와 Compose/Kubernetes 배포 정의 추가

### 9.2 읽음 처리 + unread 카운트
- 메시지 모델 `readBy` 필드 추가
- 대화 모델 `lastReadAtByUser`, `lastMessageAt`, `lastMessagePreview` 필드 추가
- API 추가:
  - `GET /chat/conversations/unread`
  - `POST /chat/conversations/{conversationId}/read`
  - `GET /chat/conversations/private?recipient=...`
- WebSocket read-receipt 이벤트(`/user/queue/private/{conversationId}/read`) 추가
- 기존 Thymeleaf 홈/개인채팅 화면에 unread 배지 및 읽음 상태 표시 반영

### 9.3 Redis Pub/Sub 기반 다중 인스턴스 WebSocket
- Redis Pub/Sub 이벤트 모델(`ChatRealtimeEvent`) 추가
- Redis publisher/subscriber 및 listener container 추가
- `APP_WEBSOCKET_REDIS_ENABLED=true` 시 WebSocket 이벤트를 Redis 경유로 릴레이
- Compose/Kubernetes에 Redis 서비스 추가

### 9.4 관측성(로그/메트릭/트레이싱)
- 전 서비스 Actuator + Prometheus + Micrometer Tracing 의존성 추가
- `/actuator/prometheus` 노출 및 traceId/spanId 로그 패턴 적용
- Compose에 Prometheus 서비스 및 스크랩 설정(`monitoring/prometheus.yml`) 추가

### 9.5 GitHub Actions 기반 CI/CD
- `.github/workflows/ci.yml`: PR/Push 시 백엔드 테스트 + 프론트 빌드
- `.github/workflows/cd.yml`: main 브랜치 Docker 이미지 빌드/푸시(서비스별 매트릭스) + 스모크체크 placeholder

---

## 10) Kubernetes 배포 검증 + Node/Pod 대시보드

### 10.1 검증 목표

- MSA 기반 Spring Boot 백엔드 모듈이 Kubernetes에서 각각 Pod로 정상 기동되는지 확인
- Node/Pod 상태를 웹 대시보드에서 관찰 가능한 솔루션 설치
- 결과 화면 캡처 및 문서화

### 10.2 테스트 일시/환경

- 테스트 일시: **2026-03-12**
- 클러스터: `kind-chat-app-kind` (single control-plane node)
- Namespace: `chat-app`

### 10.3 Backend Pod 배포/기동 확인 결과

검증 대상 백엔드 모듈:
- `eureka-server`
- `api-gateway`
- `auth-service` (oauth)
- `chat-service`
- `profile-service`

배포 상태(`READY/AVAILABLE/UPDATED`) 확인 결과:

```text
eureka-server     1     1     1
api-gateway       1     1     1
auth-service      1     1     1
chat-service      1     1     1
profile-service   1     1     1
```

Node 상태:

```text
chat-app-kind-control-plane   Ready   v1.30.0   172.25.0.2
```

Pod 상태(요약):

```text
api-gateway-...      1/1 Running
auth-service-...     1/1 Running
chat-service-...     1/1 Running
eureka-server-...    1/1 Running
profile-service-...  1/1 Running
```

### 10.4 설치한 대시보드 솔루션

- **Kubernetes Dashboard** (웹 UI)
- **metrics-server** (Node/Pod 메트릭 제공)

적용 리소스:
- `kubernetes-dashboard` namespace 및 dashboard deployment/service
- `metrics-server` deployment/service
- `dashboard-admin` ServiceAccount + `cluster-admin` ClusterRoleBinding

### 10.5 대시보드 접속/캡처 방법

포트 포워딩:

```bash
kubectl -n kubernetes-dashboard port-forward svc/kubernetes-dashboard 8443:443 --address 127.0.0.1
```

로그인 토큰 발급:

```bash
kubectl -n kubernetes-dashboard create token dashboard-admin
```

자동 캡처 스크립트:

```bash
DASHBOARD_TOKEN="$(kubectl -n kubernetes-dashboard create token dashboard-admin)" \
node scripts/capture-k8s-dashboard.cjs
```

> `scripts/capture-k8s-dashboard.cjs`는 Playwright 런타임이 필요합니다.  
> 본 저장소에서는 Playwright 컨테이너(`mcr.microsoft.com/playwright:v1.58.2-jammy`)로 실행했습니다.

컨테이너 기반 실행 예시:

```bash
DASHBOARD_TOKEN="$(kubectl -n kubernetes-dashboard create token dashboard-admin)"
docker run --rm --network host \
  -e DASHBOARD_TOKEN="$DASHBOARD_TOKEN" \
  -v "$PWD:/work" -w /work \
  mcr.microsoft.com/playwright:v1.58.2-jammy \
  bash -lc "cd /tmp && npm init -y >/dev/null 2>&1 && npm install playwright@1.58.2 >/dev/null 2>&1 && NODE_PATH=/tmp/node_modules node /work/scripts/capture-k8s-dashboard.cjs"
```

### 10.6 Dashboard 화면 캡처

#### Dashboard 로그인 화면

![Kubernetes Dashboard Login](docs/screenshots/04-k8s-dashboard-login.png)

#### Node 목록 화면

![Kubernetes Dashboard Nodes](docs/screenshots/05-k8s-dashboard-nodes.png)

#### chat-app Namespace Pod 목록 화면

![Kubernetes Dashboard Pods](docs/screenshots/06-k8s-dashboard-pods-chat-app.png)

---

## 11) Chat Backoffice (Kubernetes API 직접 조회)

### 11.1 구현 목표

- Kubernetes Dashboard 외 별도 운영 화면에서 Node/Pod 상태 모니터링
- MSA 채팅 지표(채팅방 수, 채팅 참여자 수) 동시 확인
- 운영자 계정 로그인 후 접근 가능한 백오피스 모듈 제공

### 11.2 Admin 계정

- Email: `admin@test.com`
- Password: `123456`

> 운영 환경에서는 반드시 환경변수로 교체하세요.  
> (`APP_ADMIN_AUTH_EMAIL`, `APP_ADMIN_AUTH_PASSWORD`, `APP_ADMIN_AUTH_TOKEN_SECRET`)

### 11.3 아키텍처/동작 방식

- 프론트 백오피스 경로: `/backoffice`
- Admin API: `/chat/admin/login`, `/chat/admin/me`, `/chat/admin/overview`, `/chat/admin/logout`
- Nginx 라우팅: `/chat/admin`, `/chat/me` 는 `chat-service`로 직접 프록시
- `chat-service`가 Kubernetes API를 직접 호출:
  - `GET /api/v1/nodes`
  - `GET /api/v1/namespaces/{namespace}/pods`
- `chat-service` Pod에 RBAC 적용:
  - ServiceAccount: `chat-service-admin-reader`
  - ClusterRole: `nodes`, `pods` `get/list`
  - ClusterRoleBinding 연결
- 채팅 지표:
  - `totalRoomCount`: `public` + private conversation 수
  - `participantCount`: private conversation 기준 유니크 사용자 수

### 11.4 백오피스 캡처 방법

```bash
kubectl -n chat-app port-forward svc/frontend 5173:5173 --address 127.0.0.1
```

```bash
node scripts/capture-backoffice-dashboard.cjs
```

컨테이너 기반 Playwright 실행 예시:

```bash
docker run --rm --network host \
  -e BASE_URL=http://127.0.0.1:5173 \
  -e ADMIN_EMAIL=admin@test.com \
  -e ADMIN_PASSWORD=123456 \
  -v "$PWD:/work" -w /work \
  mcr.microsoft.com/playwright:v1.58.2-jammy \
  bash -lc "cd /tmp && npm init -y >/dev/null 2>&1 && npm install playwright@1.58.2 >/dev/null 2>&1 && NODE_PATH=/tmp/node_modules node /work/scripts/capture-backoffice-dashboard.cjs"
```

### 11.5 Backoffice 화면 캡처

#### Backoffice Login

![Backoffice Login](docs/screenshots/07-backoffice-login.png)

#### Backoffice Dashboard (Node/Pod + Chat Metrics)

![Backoffice Dashboard](docs/screenshots/08-backoffice-dashboard.png)
