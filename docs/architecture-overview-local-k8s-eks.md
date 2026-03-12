# Chat App Architecture Overview (Local k8s + AWS EKS dev/prod)

## 1) Repository Reality Check (As-Is)

This repository is a Spring Boot microservice system with these runtime components:

- `eureka-server` (Service Discovery)
- `api-gateway` (edge routing for `/chat`, `/auth`, `/profile`)
- `oauth` (`auth-service`, OAuth2 + JWT cookie issue)
- `chat-service` (REST + WebSocket/STOMP + Redis pub/sub relay)
- `profile-service` (profile API + view)
- `frontend` (React + Vite build, served by Nginx)
- Data/infra dependencies: `mongodb-users`, `mongodb-profiles`, `mongodb-messages`, `redis`, `prometheus`

Key evidence in repo:

- `docker-compose.yml`: all services + MongoDB x3 + Redis + Prometheus
- `kubernetes.yml`: namespace + Deployments/Services/PVCs for the same components
- `.github/workflows/ci.yml`: backend test matrix + frontend build
- `.github/workflows/cd.yml`: Docker image build/push to Docker Hub (deployment placeholder only)

## 2) Environment Strategy

### Local (k8s)

Local environment already supports Kubernetes through:

- `deploy-all.sh`: builds JARs/images, creates `kind` cluster, loads local images, applies `kubernetes.yml`
- `kubernetes.yml`: manifests for app + Redis + MongoDB + PVCs

Operational note:

- Local manifest uses `imagePullPolicy: Never` for app images, which is correct for kind image-loading flow.

### Dev / Prod (AWS EKS)

For cloud environments, keep the same microservice decomposition but move to managed AWS primitives:

- **Compute**: EKS node groups (dev/prod separated)
- **Ingress**: AWS Load Balancer Controller + ALB (TLS termination)
- **Container Registry**: Amazon ECR
- **Data**:
  - MongoDB: Amazon DocumentDB (or Atlas) per environment
  - Redis: Amazon ElastiCache for Redis per environment
- **Secrets/Config**:
  - AWS Secrets Manager (+ External Secrets Operator) for runtime secrets
  - ConfigMap for non-sensitive env config
- **Observability**:
  - Prometheus + Grafana in-cluster, or Amazon Managed Prometheus (AMP) + Amazon Managed Grafana (AMG)
  - CloudWatch Container Insights for infra-level telemetry

Recommended cluster topology:

- Separate EKS cluster per environment (`chat-dev-eks`, `chat-prod-eks`)
- Namespace split at least `chat-app`, `monitoring`, `ingress-system`

## 3) Request / Data Flow

1. Browser hits frontend via ALB.
2. Frontend calls `/chat`, `/auth`, `/profile` through API Gateway.
3. API Gateway resolves services through Eureka (`lb://...` routes).
4. OAuth login succeeds in `auth-service`; `JWT_TOKEN` cookie is set.
5. Chat/profile services validate JWT cookie and serve APIs.
6. Chat realtime uses SockJS/STOMP endpoint (`/chat`) and Redis pub/sub relay when enabled.
7. Chat/auth/profile persist to dedicated MongoDB databases.

## 4) Tech Stack Summary

### Backend

- Java 17
- Spring Boot 3.4.x
- Spring Cloud Gateway
- Spring Cloud Netflix Eureka (server + client)
- Spring Security + OAuth2 Client
- Spring Data MongoDB
- Spring Data Redis
- WebSocket/STOMP + SockJS
- WebClient + LoadBalanced discovery calls
- JWT (`jjwt 0.12.6`)
- Micrometer + Prometheus + OpenTelemetry bridge

### Frontend

- React 18 + TypeScript
- Vite 6
- Tailwind CSS 3
- STOMP client + SockJS client
- Nginx container for static serving and reverse proxy

### Infra / Delivery

- Docker / Docker Compose
- Kubernetes manifests (`kubernetes.yml`)
- kind (local k8s flow)
- GitHub Actions CI/CD
- Prometheus scrape configuration (`monitoring/prometheus.yml`)

## 5) GitHub Actions (Current vs Target)

### Current in repo

- `ci.yml`
  - Trigger: `push`/`pull_request` on `main`
  - Runs backend tests per service matrix
  - Builds frontend
- `cd.yml`
  - Trigger: `push` on `main` and manual dispatch
  - Builds and pushes Docker images to Docker Hub
  - Has only a deployment placeholder

### Target for EKS operations

Recommended workflow split:

1. `ci.yml` (keep and strengthen)
2. `cd-dev.yml` (auto deploy to EKS dev on merge to `main`)
3. `cd-prod.yml` (manual approval + deploy to EKS prod)

Recommended deploy mechanics:

- Use GitHub OIDC -> AWS IAM Role (no long-lived AWS keys)
- Build once, push image tag by `sha`
- Update Helm values/Kustomize image tag
- `kubectl`/Helm deploy to EKS namespace
- Run smoke test (`/actuator/health`, `/chat/me`)

Recommended GitHub environments:

- `dev` (auto)
- `prod` (required reviewers / approval gate)

Required secrets/variables:

- `AWS_REGION`
- `EKS_CLUSTER_NAME_DEV`, `EKS_CLUSTER_NAME_PROD`
- `ECR_REPOSITORY_PREFIX`
- App secrets in AWS Secrets Manager:
  - `GOOGLE_CLIENT_ID`
  - `GOOGLE_CLIENT_SECRET`
  - `JWT_SECRET` (should replace hardcoded key)
  - Mongo/Redis connection URIs

## 6) Gap List Before Production

1. `JWT` secret is hardcoded in code (`JwtUtil`) and should be externalized.
2. OAuth redirect URI is fixed to `http://localhost:9999/...`; environment-specific redirect config is required for dev/prod domains.
3. Current Kubernetes manifest has no Ingress/HPA/PDB/NetworkPolicy.
4. Current CD does not deploy to any Kubernetes cluster.
5. Image source differs by env:
   - local kind: local images + `imagePullPolicy: Never`
   - EKS: pull from ECR with regular `IfNotPresent`/`Always`

## 7) Infrastructure Diagram (SVG)

Use this file:

- `docs/infra-architecture-local-k8s-eks.svg`

