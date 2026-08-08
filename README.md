# StudyNotion LMS — Full Stack

AI-Powered Learning Management System
**Stack:** React 18 + Spring Boot 3.2 + PostgreSQL + Docker

---

## Project Structure (Same as Flow project)

```
studynotion-fullstack/
├── docker-compose.yml          ← ONE command to run everything
├── .env                        ← Your secrets (fill this in)
├── .env.example                ← Template
│
├── studynotion-backend/        ← Java Spring Boot
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/studynotion/
│       ├── controller/         ← REST API endpoints
│       ├── service/            ← Business logic + AI features
│       ├── repository/         ← Database queries (JPA)
│       ├── entity/             ← Database tables
│       ├── security/           ← JWT auth
│       └── config/             ← Spring Security, Cloudinary
│
└── studynotion-frontend/       ← React 18
    ├── Dockerfile
    ├── nginx.conf              ← Proxies /api → backend
    └── src/
        ├── services/apis.js    ← All API endpoint URLs
        └── ...
```

---

## Quick Start — 3 Steps Only

### Step 1: Fill in .env file
```bash
cp .env.example .env
# Edit .env with your Cloudinary, Razorpay, Gmail, Anthropic keys
```

### Step 2: Run everything
```bash
docker compose up --build
```

### Step 3: Open browser
- Frontend: http://localhost:3000
- Backend API: http://localhost:8080/api/v1
- API Docs: http://localhost:8080/swagger-ui.html

---

## How it works (like Flow project)

```
Browser → Nginx (port 3000)
            │
            ├── /          → React app (frontend)
            └── /api/*     → Spring Boot (backend:8080)
                                │
                                ├── PostgreSQL (DB)
                                ├── Cloudinary (videos)
                                ├── Razorpay (payments)
                                └── Anthropic AI (4 AI features)
```

---

## AI Features
1. **Smart Recommendations** — `GET /api/v1/ai/recommendations`
2. **Course Summary Generator** — `POST /api/v1/ai/generate-summary`
3. **In-Course Chatbot** — `POST /api/v1/ai/chat/{courseId}`
4. **Auto Quiz Generator** — `POST /api/v1/ai/generate-quiz`

---

## Deploy

**Backend → Render** | **Frontend → Vercel**

See `studynotion-backend/COMPLETE_GUIDE.md` for full deployment steps.
