# Docker — Complete Guide for Enterprise RAG Wrapper API

---

## What is Docker?

Docker is a tool that packages your application and everything it needs (Java, libraries, config) into a single portable unit called a **container**.

Without Docker:
```
Developer A runs it on Windows → works
Developer B runs it on Mac → "it doesn't work on my machine"
Production server runs Linux → breaks
```

With Docker:
```
Everyone runs the same container → works everywhere
```

---

## Docker Workflow Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                        YOUR PROJECT                                 │
│                                                                     │
│   Source Code + Dockerfile + docker-compose.yml                    │
└──────────────────────────┬──────────────────────────────────────────┘
                           │
                           │ docker build
                           ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        DOCKER IMAGE                                 │
│                                                                     │
│   Java 17 (Amazon Corretto)                                        │
│   + wrapper-api-0.0.1-SNAPSHOT.jar                                 │
│   + app.jar                                                        │
│   + non-root user (appuser)                                        │
│                                                                     │
│   robin419/rag-wrapper-api:latest                                  │
└──────────────────────────┬──────────────────────────────────────────┘
                           │
              ┌────────────┴────────────┐
              │                         │
              │ docker push             │ docker run / docker-compose up
              ▼                         ▼
┌─────────────────────┐    ┌─────────────────────────────────────────┐
│    DOCKER HUB       │    │              CONTAINERS                 │
│                     │    │                                         │
│  robin419/          │    │  ┌──────────────────┐                  │
│  rag-wrapper-api    │    │  │  rag-wrapper-api  │ :8080           │
│  :latest            │    │  │  (Spring Boot)    │                  │
│                     │    │  └────────┬─────────┘                  │
│  Anyone can pull    │    │           │                             │
│  and run this       │    │  ┌────────▼─────────┐                  │
│  image              │    │  │   rag-postgres    │ :5432           │
│                     │    │  │   (pgvector)      │                  │
└─────────────────────┘    │  └────────┬─────────┘                  │
                           │           │                             │
                           │  ┌────────▼─────────┐                  │
                           │  │     ollama        │ :11434          │
                           │  │  (embeddings)     │                  │
                           │  └──────────────────┘                  │
                           └─────────────────────────────────────────┘
```

---

## Files in This Project

| File | Purpose |
|---|---|
| `Dockerfile` | Instructions to build the app image |
| `docker-compose.yml` | Runs all 3 services together (app + postgres + ollama) |
| `.dockerignore` | Excludes unnecessary files from the image |

---

## Dockerfile Explained

```dockerfile
# Use Amazon Corretto Java 17 as base
FROM amazoncorretto:17-alpine

WORKDIR /app

# Create non-root user (security best practice)
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the pre-built JAR
COPY target/wrapper-api-0.0.1-SNAPSHOT.jar app.jar

# Set ownership
RUN chown appuser:appgroup app.jar

# Run as non-root
USER appuser

# App runs on port 8080
EXPOSE 8080

# Health check — verifies app is responding
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -q --spider http://localhost:8080/api/rag/health || exit 1

# Start the Spring Boot app
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## Docker Containers in This Project

### 1. `rag-postgres` — PostgreSQL + pgvector
```
Image:    pgvector/pgvector:pg16
Port:     5432
Database: rag_db
User:     postgres
Password: postgres
Purpose:  Stores documents, vector chunks, and users table
```

### 2. `ollama` — Local Embedding Model
```
Image:    ollama/ollama
Port:     11434
Model:    nomic-embed-text (768-dim vectors)
Purpose:  Converts text chunks and questions into vector embeddings
```

### 3. `rag-wrapper-api` — Spring Boot App
```
Image:    robin419/rag-wrapper-api:latest
Port:     8080
Purpose:  REST API — handles upload, ask, auth, OAuth2
```

---

## Step-by-Step Commands

### Step 1 — Build the JAR

```powershell
mvn package -DskipTests
```

This creates `target/wrapper-api-0.0.1-SNAPSHOT.jar`

---

### Step 2 — Build the Docker Image

```powershell
docker build -t robin419/rag-wrapper-api:latest .
```

What this does:
1. Reads `Dockerfile`
2. Pulls `amazoncorretto:17-alpine` base image
3. Copies the JAR into the image
4. Creates `robin419/rag-wrapper-api:latest` image locally

Verify:
```powershell
docker images robin419/rag-wrapper-api
```

---

### Step 3 — Login to Docker Hub

```powershell
docker login
# Enter your Docker Hub username: robin419
# Enter your password
```

---

### Step 4 — Push to Docker Hub

```powershell
docker push robin419/rag-wrapper-api:latest
```

Image is now publicly available at:
```
https://hub.docker.com/r/robin419/rag-wrapper-api
```

Anyone can pull it with:
```powershell
docker pull robin419/rag-wrapper-api:latest
```

---

### Step 5 — Run Individual Containers (Manual)

```powershell
# Start PostgreSQL
docker run -d --name rag-postgres \
  -e POSTGRES_DB=rag_db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  pgvector/pgvector:pg16

# Start Ollama
docker run -d --name ollama \
  -p 11434:11434 \
  ollama/ollama

# Pull the embedding model inside Ollama
docker exec ollama ollama pull nomic-embed-text

# Start the app
docker run -d --name rag-wrapper-api \
  -p 8080:8080 \
  -e GROQ_API_KEY=your-groq-key \
  -e GOOGLE_CLIENT_ID=your-google-client-id \
  -e GOOGLE_CLIENT_SECRET=your-google-client-secret \
  robin419/rag-wrapper-api:latest
```

---

### Step 6 — Run All Together with Docker Compose

```powershell
# Set required env vars first
$env:GROQ_API_KEY="your-groq-key"
$env:GOOGLE_CLIENT_ID="your-google-client-id"
$env:GOOGLE_CLIENT_SECRET="your-google-client-secret"

# Start all services
docker-compose up -d

# Check all are running
docker-compose ps

# View logs
docker-compose logs -f app

# Stop all
docker-compose down
```

---

## Useful Docker Commands

### Container Management

```powershell
# List running containers
docker ps

# List all containers (including stopped)
docker ps -a

# Start existing container
docker start rag-postgres
docker start ollama

# Stop a container
docker stop rag-wrapper-api

# Remove a container
docker rm rag-wrapper-api

# View container logs
docker logs rag-wrapper-api
docker logs -f rag-wrapper-api   # follow live logs
```

### Image Management

```powershell
# List all images
docker images

# Remove an image
docker rmi robin419/rag-wrapper-api:latest

# Pull latest image from Docker Hub
docker pull robin419/rag-wrapper-api:latest

# Tag image with version
docker tag robin419/rag-wrapper-api:latest robin419/rag-wrapper-api:v1.0.0
docker push robin419/rag-wrapper-api:v1.0.0
```

### Database Access

```powershell
# Connect to PostgreSQL inside container
docker exec -it rag-postgres psql -U postgres -d rag_db

# Run a SQL query directly
docker exec -it rag-postgres psql -U postgres -d rag_db -c "SELECT * FROM users;"
docker exec -it rag-postgres psql -U postgres -d rag_db -c "SELECT COUNT(*) FROM vector_chunks;"
```

### Ollama Model Management

```powershell
# Pull embedding model
docker exec ollama ollama pull nomic-embed-text

# List downloaded models
docker exec ollama ollama list

# Test embedding model
docker exec ollama ollama run nomic-embed-text
```

---

## Environment Variables Required

| Variable | Required | Description |
|---|---|---|
| `GROQ_API_KEY` | ✅ Yes | Groq API key for LLaMA LLM |
| `GOOGLE_CLIENT_ID` | For OAuth2 | Google OAuth2 client ID |
| `GOOGLE_CLIENT_SECRET` | For OAuth2 | Google OAuth2 client secret |
| `GITHUB_CLIENT_ID` | Optional | GitHub OAuth2 client ID |
| `GITHUB_CLIENT_SECRET` | Optional | GitHub OAuth2 client secret |

Set permanently on Windows:
```powershell
[System.Environment]::SetEnvironmentVariable("GROQ_API_KEY", "your-key", "User")
[System.Environment]::SetEnvironmentVariable("GOOGLE_CLIENT_ID", "your-id", "User")
[System.Environment]::SetEnvironmentVariable("GOOGLE_CLIENT_SECRET", "your-secret", "User")
```

---

## Docker Hub Image

```
Repository: robin419/rag-wrapper-api
Tag:        latest
URL:        https://hub.docker.com/r/robin419/rag-wrapper-api
```

Pull and run on any machine:
```powershell
docker pull robin419/rag-wrapper-api:latest
docker run -p 8080:8080 \
  -e GROQ_API_KEY=your-key \
  robin419/rag-wrapper-api:latest
```

---

## Troubleshooting

| Problem | Cause | Fix |
|---|---|---|
| `Connection refused 5432` | Postgres container not running | `docker start rag-postgres` |
| `Connection refused 11434` | Ollama container not running | `docker start ollama` |
| `TLS handshake timeout` on build | Slow network to Docker Hub | Retry `docker build` or use VPN |
| `image not found` on push | Not logged in | Run `docker login` first |
| `port already in use` | Another process on same port | `docker ps` and stop conflicting container |
| Build fails — JAR not found | Forgot to run Maven build | Run `mvn package -DskipTests` first |
| App starts but DB fails | Wrong DB host in docker-compose | Use `postgres` (service name) not `localhost` |
