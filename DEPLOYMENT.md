# StudyNotion — Deployment Guide

Backend → **Render** (Docker + managed Postgres) · Frontend → **Vercel** (static CRA build).

---

## 0. Before you start

### Secrets are local-only — keep it that way
`.env` (root) and `studynotion-frontend/.env` are **gitignored and were never committed** — verified. Never `git add -f` them. Everything secret is supplied at deploy time through the Render / Vercel dashboards.

The test-tier keys currently in your local `.env` (Razorpay `rzp_test_…`, Cloudinary, Gemini, Gmail app password) were shared in chat, so rotate them before real traffic — not urgent, but do it:

| Secret | Rotate at |
|---|---|
| Gmail app password | Google Account → Security → App passwords → revoke + create new |
| Cloudinary API secret | cloudinary.com → Settings → Security → regenerate |
| Razorpay key/secret | dashboard.razorpay.com → Settings → API Keys → regenerate (use **Live** keys for production) |
| Gemini API key | aistudio.google.com/apikey → delete + create |
| `JWT_SECRET` | Render generates a fresh one automatically (see `render.yaml`) |

### One-time sanity check
```bash
cd studynotion-backend && mvn -q -DskipTests package
cd ../studynotion-frontend && CI=false npm run build
```
Both are already verified to pass (and both Docker images build).

---

## 1. Database

Render allows only **one free Postgres per account**. If that slot is already used by another project, get a free Postgres elsewhere instead — this is what `render.yaml` expects by default (`DB_HOST` etc. are manual `sync:false` vars, not `fromDatabase`).

**neon.tech** (recommended — free, no card, instant):
1. Sign up → **New Project** → name it `studynotion` → pick a region.
2. Project → **Connection Details** — copy: host (looks like `ep-xxxx-xxxx.region.aws.neon.tech`), database name, role/username, password.
3. Keep these handy for the Render step below. Neon requires SSL — `render.yaml` already appends `?sslmode=require` via `DB_PARAMS`.

If you'd rather use Render's own free Postgres and don't have one in use elsewhere, add back a `databases:` block to `render.yaml` (see git history) and wire `DB_*` with `fromDatabase` instead.

## 2. Backend on Render

### Option A — Blueprint (recommended)
1. Push this repo to GitHub.
2. Render Dashboard → **New → Blueprint** → select the repo. Render reads [`render.yaml`](render.yaml) and creates `studynotion-backend` (Docker web service, health-checked at `/api/v1/health`).
3. When prompted, fill the `sync:false` env vars:
   ```
   DB_HOST, DB_NAME, DB_USERNAME, DB_PASSWORD    # from Neon (step 1)
   MAIL_USERNAME, MAIL_PASSWORD
   CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET
   RAZORPAY_KEY_ID, RAZORPAY_KEY_SECRET
   GEMINI_API_KEY
   FRONTEND_URL              # set after step 3 below — e.g. https://studynotion-xyz.vercel.app
   CORS_ALLOWED_ORIGINS      # same value (comma-separate if more than one)
   ```
   `JWT_SECRET` is generated automatically.
4. Deploy. First build ≈ 5-8 min. Backend URL: `https://studynotion-backend-XXXX.onrender.com`.

### Option B — manual
New → Web Service → repo → Runtime **Docker**, Root Directory `studynotion-backend`, Health Check Path `/api/v1/health`. Set `DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD` from Neon (or your own Postgres), plus all the secrets above and `JWT_SECRET`.

### Notes
- Free Postgres is deleted after 30 days and the web service sleeps after 15 min idle (first request ~30 s). Upgrade both before launch.
- Hibernate auto-creates all tables (`JPA_DDL_AUTO=update`). Then seed categories once:
  ```sql
  INSERT INTO categories (name, description) VALUES
    ('Web Development', 'Web development courses'),
    ('Data Science', 'Data science and ML courses'),
    ('Mobile Development', 'Android and iOS courses');
  ```
  (Render DB → Connect → PSQL command.)
- Gmail SMTP on Render: requires a Google **App Password** (2-Step Verification must be on).

---

## 3. Frontend on Vercel

1. Vercel → **New Project** → import the repo.
2. **Root Directory: `studynotion-frontend`**. Framework auto-detects as Create React App; [`vercel.json`](studynotion-frontend/vercel.json) supplies the SPA rewrite and a `CI=false` build (CRA fails the build on lint warnings otherwise).
3. Environment variable:
   ```
   REACT_APP_BASE_URL = https://<your-render-backend>.onrender.com/api/v1
   ```
   (Overrides the committed `studynotion-frontend/.env.production` default. `REACT_APP_*` vars are baked in at build time — redeploy after changing.)
4. Deploy. Copy the resulting URL.

## 4. Wire the two together
1. Render → `studynotion-backend` → Environment → set `FRONTEND_URL` and `CORS_ALLOWED_ORIGINS` to the Vercel URL → save (redeploys).
2. Razorpay dashboard → add the Vercel URL to allowed domains if you have domain restrictions on.
3. Hard-refresh the frontend and run the smoke test below.

---

## 5. Local full stack (`docker compose`)

```bash
cp .env.example .env          # fill in real values
docker compose up --build
# frontend  → http://localhost:3000   (nginx proxies /api → backend)
# backend   → http://localhost:8080/api/v1/health
```
After a volume reset (`docker compose down -v`) re-seed the categories (SQL above).

Rebuild cleanly when source changed:
```bash
docker compose down && docker system prune -f && docker compose up --build
```

---

## 6. Post-deploy smoke test

| Flow | Check |
|---|---|
| Health | `GET /api/v1/health` → `{"status":"UP","db":"UP"}` |
| Auth | Sign up (OTP email arrives) → log in → JWT stored |
| Catalog | Home + Catalog pages list published courses |
| Instructor | Create course → add section → add lecture (video → Cloudinary) → Publish |
| Student | Open course → Buy Now → Razorpay test card `4111 1111 1111 1111` → lands on Enrolled Courses |
| Learning | Open enrolled course → play video → Mark as Complete → progress % updates |
| Reviews | Submit a rating → appears on course + on the Reviews carousel |
| AI (API only, no UI yet) | `POST /api/v1/ai/generate-quiz` with a Bearer token → JSON quiz |

Razorpay test cards: https://razorpay.com/docs/payments/payments/test-card-details/

---

## 7. What changed in this production pass

**Backend**
- Fixed `LazyInitializationException` across `addSection`/`addSubSection`/`getReviews`/`getEnrolledCourses`/AI — every response builder now materialises collections inside its transaction; added `@Transactional` where it was missing.
- Removed `@PreAuthorize("hasRole(...)")`; role checks are explicit in the service layer, controllers use `isAuthenticated()`.
- Fixed `CreateCourseRequest.categoryId` (String → Long via `parseCategoryId`) — the project previously **did not compile**.
- `tag`/`instructions` accepted as JSON strings from the multipart form and parsed server-side.
- Payment: `courseIds` no longer `ClassCastException` (Integer→Long); `capturePayment` is now transactional; signature check is constant-time; enrollment writes the owning side of the M:N so it actually persists.
- Instructors are no longer recorded as "enrolled students" of their own courses.
- AI endpoints now require authentication.
- CORS driven by `FRONTEND_URL` + `CORS_ALLOWED_ORIGINS`, allows `*.vercel.app` previews.
- Added `/api/v1/health`; `application.yml` fully env-driven; `forward-headers-strategy` for Render's proxy.

**Frontend**
- `courseDetailsAPI`: section create/update/delete read `data` (not the non-existent `updatedCourse`); `fetchCourseDetails` reads the object, not `data[0]`.
- `SubsectionModal`: correct multipart field names (`video`, `subSectionId`).
- `PublishCourse`: uses `course.id`, dropped the call to the undefined `ADD_COURSE_TO_CATEGORY_API`.
- Payment: reads `orderResponse.data.data`, uses backend-provided Razorpay key, removed the duplicate order call.
- `CourseDetails`: tolerant of `instructions` as array; enrolled check works with numeric ids.
- Backend responses include both `id`/`_id` and `subSection`/`subSections` for compatibility.

**Not done (needs product decisions / new UI):**
- No frontend UI for the 4 AI features — backend endpoints are ready and testable via API.
- Admin panel is a stub in the original app.
- Free-tier Render/Postgres limits — upgrade before launch.
