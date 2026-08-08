# StudyNotion LMS — Complete Deployment & Interview Guide

---

## PART 1: PROJECT STRUCTURE (What we built)

```
studynotion-backend/
├── Dockerfile
├── render.yaml
├── pom.xml
└── src/main/java/com/studynotion/
    ├── StudyNotionApplication.java       ← Main entry point
    ├── config/
    │   ├── SecurityConfig.java           ← Spring Security + CORS + JWT
    │   ├── CloudinaryConfig.java         ← Cloudinary bean
    │   └── AppConfig.java                ← ObjectMapper bean
    ├── controller/
    │   ├── AuthController.java           ← /api/v1/auth/*
    │   ├── CourseController.java         ← /api/v1/course/*
    │   ├── PaymentController.java        ← /api/v1/payment/*
    │   ├── ProfileController.java        ← /api/v1/profile/*
    │   └── AIController.java             ← /api/v1/ai/*
    ├── service/
    │   ├── AuthService.java
    │   ├── CourseService.java
    │   ├── PaymentService.java
    │   ├── ProfileService.java
    │   ├── CategoryService.java
    │   ├── RatingService.java
    │   ├── CourseProgressService.java
    │   ├── CloudinaryService.java
    │   ├── EmailService.java
    │   ├── AIService.java                ← All 4 AI features
    │   └── CustomUserDetailsService.java
    ├── repository/                       ← Spring Data JPA
    ├── entity/                           ← @Entity classes (DB tables)
    ├── dto/request/ + dto/response/      ← Input/Output objects
    ├── security/                         ← JwtUtil + JwtAuthFilter
    ├── exception/                        ← AppException + GlobalHandler
    └── util/SecurityUtils.java
```

---

## PART 2: STEP-BY-STEP LOCAL SETUP

### Prerequisites
- Java 17+  → `java -version`
- Maven 3.9+ → `mvn -version`
- PostgreSQL → Download from postgresql.org
- Git

### Step 1: Create PostgreSQL Database
```sql
-- Open psql or pgAdmin and run:
CREATE DATABASE studynotion;
CREATE USER studynotion_user WITH PASSWORD 'yourpassword';
GRANT ALL PRIVILEGES ON DATABASE studynotion TO studynotion_user;
```

### Step 2: Clone and Configure
```bash
git clone <your-repo-url>
cd studynotion-backend

# Copy env template
cp .env.example .env
# Fill in all values in .env
```

### Step 3: Update application.yml for local
The app reads environment variables. For local dev, set them in your IDE
or create a `.env` and use spring-dotenv, OR just edit application.yml directly:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/studynotion
    username: postgres
    password: yourpassword
```

### Step 4: Run
```bash
mvn clean install
mvn spring-boot:run

# App starts at: http://localhost:8080
# Hibernate will auto-create all tables on first run!
```

### Step 5: Test APIs with Postman
Import these sample requests:

**Send OTP:**
```
POST http://localhost:8080/api/v1/auth/sendotp
Content-Type: application/json
{ "email": "test@gmail.com" }
```

**Signup:**
```
POST http://localhost:8080/api/v1/auth/signup
{ "firstName":"John","lastName":"Doe","email":"test@gmail.com",
  "password":"Test@1234","confirmPassword":"Test@1234",
  "accountType":"Student","otp":"123456" }
```

**Login:**
```
POST http://localhost:8080/api/v1/auth/login
{ "email":"test@gmail.com","password":"Test@1234" }
→ Returns JWT token. Use in Authorization: Bearer <token> header
```

---

## PART 3: FRONTEND CHANGES (MINIMAL!)

Your React frontend barely needs changes. Just update the API base URL.

### Find in frontend: src/services/apis.js or similar
```javascript
// OLD (Node.js):
const BASE_URL = "http://localhost:4000/api/v1"

// NEW (Spring Boot):
const BASE_URL = "http://localhost:8080/api/v1"
```

### Update .env in frontend:
```
REACT_APP_BASE_URL=http://localhost:8080/api/v1
# For production:
REACT_APP_BASE_URL=https://studynotion-backend.onrender.com/api/v1
```

That's it! All endpoints are same. The Spring Boot backend mirrors the
same API routes as Node.js.

---

## PART 4: DEPLOY TO RENDER (Backend)

### Step 1: Push code to GitHub
```bash
git init
git add .
git commit -m "Initial Spring Boot backend"
git remote add origin https://github.com/yourusername/studynotion-backend
git push -u origin main
```

### Step 2: Create Render Account
Go to render.com → Sign up → Connect GitHub

### Step 3: Create PostgreSQL Database on Render
1. Render Dashboard → "New" → "PostgreSQL"
2. Name: `studynotion-db`
3. Plan: Free
4. Copy the "Internal Database URL" (you'll need it)

### Step 4: Create Web Service on Render
1. Render Dashboard → "New" → "Web Service"
2. Connect your GitHub repo
3. Settings:
   - Name: `studynotion-backend`
   - Runtime: **Docker**
   - Branch: `main`
   - Plan: Free

### Step 5: Set Environment Variables on Render
Go to Environment tab → Add these:
```
DATABASE_URL=<paste Internal Database URL from Step 3>
JWT_SECRET=<any 32+ char random string>
MAIL_USERNAME=your@gmail.com
MAIL_PASSWORD=<Gmail App Password>
CLOUDINARY_CLOUD_NAME=xxx
CLOUDINARY_API_KEY=xxx
CLOUDINARY_API_SECRET=xxx
RAZORPAY_KEY_ID=rzp_test_xxx
RAZORPAY_KEY_SECRET=xxx
ANTHROPIC_API_KEY=sk-ant-xxx
FRONTEND_URL=https://studynotion-lms.vercel.app
```

### Step 6: Get Gmail App Password
1. Google Account → Security → 2-Step Verification (enable)
2. Security → App Passwords → Create password for "Mail"
3. Use that 16-char password as MAIL_PASSWORD

### Step 7: Deploy!
Render will automatically build Docker image and deploy.
Your backend URL: `https://studynotion-backend.onrender.com`

**Note:** Free tier sleeps after 15 min inactivity. First request takes ~30 sec.

---

## PART 5: DEPLOY TO VERCEL (Frontend)

### Step 1: Update frontend .env.production
```
REACT_APP_BASE_URL=https://studynotion-backend.onrender.com/api/v1
```

### Step 2: Push frontend to GitHub

### Step 3: Deploy on Vercel
1. vercel.com → Sign up → "New Project"
2. Import your frontend GitHub repo
3. Framework: Create React App
4. Add environment variable:
   `REACT_APP_BASE_URL` = `https://studynotion-backend.onrender.com/api/v1`
5. Deploy!

Your frontend URL: `https://studynotion-lms.vercel.app`

### Step 4: Update FRONTEND_URL on Render
Go back to Render → Environment → Update:
`FRONTEND_URL=https://studynotion-lms.vercel.app`
→ Redeploy backend

---

## PART 6: COMPLETE INTERVIEW Q&A

### ── JAVA/SPRING BOOT BASICS ──

**Q1: @SpringBootApplication kya karta hai?**
A: Teen annotations ka combination hai:
- `@Configuration` → Spring beans define karne ke liye
- `@EnableAutoConfiguration` → Spring Boot automatically configure karta hai (DataSource, Security, etc.)
- `@ComponentScan` → Us package aur sub-packages mein @Component, @Service, @Repository scan karta hai

**Q2: Spring Boot aur Spring Framework mein kya difference hai?**
A: Spring Boot, Spring Framework ke upar bana hai. Spring mein sabkuch manually configure karna padta tha — XML files, beans, etc. Spring Boot mein "convention over configuration" hai — auto-configuration hoti hai, embedded Tomcat milta hai, starter dependencies se sab manage hota hai. StudyNotion mein maine spring-boot-starter-web use kiya jo automatically Tomcat, Jackson, MVC sab configure kar deta hai.

**Q3: Dependency Injection (DI) kya hai? Spring mein kaise use kiya?**
A: DI matlab hai — ek class dusri class ka object khud create nahi karti, bahar se inject hota hai. Spring IoC container sab manage karta hai. Maine `@RequiredArgsConstructor` (Lombok) use kiya jo constructor injection karta hai. Example: AuthController mein AuthService inject hua — Spring automatically dependency provide karta hai.

**Q4: @RestController vs @Controller?**
A: `@Controller` traditional MVC ke liye — view (HTML) return karta hai. `@RestController` = `@Controller` + `@ResponseBody` — har method automatically JSON return karta hai. StudyNotion pure REST API hai isliye `@RestController` use kiya.

**Q5: @Service, @Repository, @Component mein difference?**
A: Teeno Spring beans hain, lekin semantic difference hai:
- `@Component` → generic bean
- `@Service` → business logic layer (AuthService, CourseService)
- `@Repository` → database layer, plus exception translation karta hai (DataAccessException)
Clarity ke liye alag annotations use karte hain.

### ── SPRING SECURITY + JWT ──

**Q6: Spring Security kaise kaam karta hai project mein?**
A: SecurityFilterChain define ki jisme:
1. CSRF disable kiya (REST API hai, stateless)
2. CORS configure kiya (Vercel frontend allow ki)
3. Session STATELESS rakhi (JWT use karte hain)
4. Public endpoints permit kiye (login, signup, getAllCourses)
5. Rest ke liye authentication required
6. `JwtAuthFilter` add kiya before `UsernamePasswordAuthenticationFilter`

**Q7: JWT ka flow explain karo.**
A: 
1. User login karta hai → `AuthService.login()` called
2. Password BCrypt se verify hota hai
3. `JwtUtil.generateToken()` se token banta hai — header.payload.signature
4. Token mein: email, userId, accountType (role) hota hai
5. Client token store karta hai (localStorage/cookie)
6. Agle request pe `Authorization: Bearer <token>` header bhejta hai
7. `JwtAuthFilter` intercept karta hai → token extract karta hai → validate karta hai → SecurityContext mein set karta hai
8. Controller mein `@PreAuthorize("hasRole('INSTRUCTOR')")` role check karta hai

**Q8: BCrypt kya hai? Kyu use kiya?**
A: BCrypt ek one-way hashing algorithm hai password encryption ke liye. Features:
- Salt automatically add karta hai (har hash unique hota hai)
- Computationally expensive — brute force attacks slow karta hai
- `passwordEncoder.encode("password")` se hash, `passwordEncoder.matches()` se verify
- Salt rounds = 10 (StudyNotion mein) — balance between security and performance

**Q9: `@PreAuthorize` kaise kaam karta hai?**
A: Spring Security Expression-based Access Control hai. `@EnableMethodSecurity` enable karna padta hai. Roles format: `ROLE_STUDENT`, `ROLE_INSTRUCTOR`, `ROLE_ADMIN`. `hasRole('INSTRUCTOR')` automatically `ROLE_` prefix check karta hai. JWT se role extract hokar `GrantedAuthority` set hoti hai.

### ── DATABASE / JPA / HIBERNATE ──

**Q10: JPA aur Hibernate mein kya relation hai?**
A: JPA (Java Persistence API) ek specification/interface hai — defines karta hai ki ORM kaise hona chahiye. Hibernate JPA ka most popular implementation hai. Spring Data JPA Hibernate ke upar ek abstraction layer hai — `findById()`, `save()`, `findAll()` automatically implement hote hain bina SQL likhe.

**Q11: @Entity annotations explain karo.**
A: 
- `@Entity` → ye class ek database table hai
- `@Table(name="users")` → table ka naam
- `@Id` → primary key
- `@GeneratedValue(strategy=IDENTITY)` → auto-increment
- `@Column(nullable=false, unique=true)` → constraints
- `@OneToMany`, `@ManyToMany` → relationships
- `@CreationTimestamp` → automatically set on insert

**Q12: Lazy vs Eager loading kya hai? Kahan use kiya?**
A: 
- **Eager**: Related data turant load hota hai (JOIN query)
- **Lazy**: Jab actually access karo tab load hota hai (separate query)
Default: `@OneToMany` = Lazy, `@ManyToOne` = Eager.
StudyNotion mein: Course load karte time `studentsEnrolled` Lazy rakha — warna har course ke saath sab enrolled students load ho jaate, N+1 problem hota. Instructor details Eager rakha kyunki course card pe hamesha dikhta hai.

**Q13: @Transactional kab aur kyon use kiya?**
A: Jab ek operation mein multiple DB writes hon — sab succeed ya sab rollback. Example: `verifyPaymentAndEnroll()`:
1. Har course mein student add karo
2. User ke courses mein add karo
3. CourseProgress create karo
4. Email bhejo
Agar step 2 fail ho toh step 1 bhi rollback hona chahiye. `@Transactional` ye guarantee deta hai.

**Q14: MongoDB se PostgreSQL kyun switch kiya?**
A: LMS ke liye relational data bahut suitable hai — User-Course enrollment, Section-SubSection hierarchy, Payment records — ye sab clearly defined relationships hain. PostgreSQL mein:
- ACID compliance guaranteed hai
- Complex JOINs efficient hain
- Foreign key constraints data integrity ensure karte hain
- Spring JPA/Hibernate ke saath seamlessly kaam karta hai

### ── AI FEATURES ──

**Q15: AI features kaise implement kiye?**
A: Anthropic Claude API use kiya HTTP calls se (`OkHttpClient`). 4 features:
1. **Course Summary Generator** — Instructor course title + topics deta hai, AI description, learning outcomes, tags generate karta hai
2. **Smart Recommendations** — Student enrolled courses/categories AI ko bhejo, available courses mein se best 4 suggest karta hai
3. **Course Chatbot** — Course content context ke saath AI chatbot, student questions answer karta hai
4. **Quiz Generator** — Section content se MCQ questions automatically generate

Architecture: `AIService` → `callAnthropicAPI()` → OkHttp POST → Parse JSON response

**Q16: AI calls async kyon nahi kiye saare?**
A: Quiz aur Chatbot synchronous hain kyunki user response ka wait karta hai (real-time). Course summary generation `@Async` se kar sakte hain kyunki instructor immediately result nahi chahiye. Email sending pehle se `@Async` hai — user ko wait nahi karana.

### ── SYSTEM DESIGN ──

**Q17: Application architecture kya hai?**
A: 3-tier architecture:
- **Presentation Layer**: React frontend (Vercel)
- **Business Logic Layer**: Spring Boot REST API (Render)  
- **Data Layer**: PostgreSQL database (Render managed DB)
External services: Cloudinary (media), Razorpay (payments), Gmail SMTP (email), Anthropic API (AI)

**Q18: CORS kya hai? Kaise handle kiya?**
A: Cross-Origin Resource Sharing — browser security policy. Frontend (vercel.app) different origin se backend (render.com) ko call karta hai. `SecurityConfig` mein `CorsConfigurationSource` define kiya:
- Specific origins allow kiye (Vercel URL + localhost:3000)
- Methods: GET, POST, PUT, DELETE, OPTIONS
- `allowCredentials: true` cookies ke liye
`@CrossOrigin` per-controller bhi laga sakte hain lekin global config better hai.

**Q19: Project mein security best practices kya follow kiye?**
A: 
- Passwords BCrypt hashed (never plain text)
- JWT tokens short-lived (24 hours)
- Role-based access control (Student/Instructor/Admin)
- Input validation `@Valid` + Bean Validation
- Global exception handler — stack trace expose nahi hota
- Environment variables mein secrets (no hardcoding)
- Non-root Docker user
- HTTPS on production (Render default)
- OTP expiry (5 minutes)

**Q20: Razorpay payment flow explain karo.**
A: 
1. Frontend: User "Buy Now" click karta hai
2. Backend: `capturePayment()` → Razorpay mein order create hota hai → orderId, amount return
3. Frontend: Razorpay checkout modal open hota hai
4. User: Payment complete karta hai
5. Razorpay: `payment_id`, `order_id`, `signature` return karta hai
6. Backend: `verifyPayment()` → HMAC-SHA256 se signature verify → enroll student → emails send

Security: Signature verification ensures payment actually Razorpay se aaya, tamper nahi hua.

---

## PART 7: RESUME BULLETS (Copy-paste ready)

```
• Built enterprise-grade LMS backend using Java 17, Spring Boot 3.2, Spring Security
  with JWT authentication and role-based access control (Student/Instructor/Admin)

• Designed PostgreSQL relational schema using JPA/Hibernate with proper entity 
  relationships, lazy loading, and transactional consistency

• Integrated Anthropic Claude AI API to build 4 features: smart course recommendations,
  AI course summary generator, in-course Q&A chatbot, and auto quiz generator

• Implemented Razorpay payment gateway with HMAC-SHA256 signature verification
  and transactional enrollment processing across multiple courses

• Built Cloudinary media pipeline supporting 100MB video uploads with async email
  notifications via Spring Mail (Gmail SMTP)

• Containerized application with Docker multi-stage builds and deployed backend 
  on Render with managed PostgreSQL, frontend on Vercel

• Achieved clean 3-layer architecture (Controller → Service → Repository) with
  centralized exception handling via @ControllerAdvice
```

---

## PART 8: QUICK REFERENCE — All API Endpoints

| Method | URL | Auth | Description |
|--------|-----|------|-------------|
| POST | /api/v1/auth/sendotp | Public | Send OTP |
| POST | /api/v1/auth/signup | Public | Register |
| POST | /api/v1/auth/login | Public | Login |
| POST | /api/v1/auth/changepassword | Auth | Change password |
| POST | /api/v1/auth/reset-password-token | Public | Forgot password |
| GET | /api/v1/course/getAllCourses | Public | All courses |
| POST | /api/v1/course/getCourseDetails | Public | Course details |
| POST | /api/v1/course/createCourse | Instructor | Create course |
| POST | /api/v1/course/addSection | Instructor | Add section |
| POST | /api/v1/course/addSubSection | Instructor | Add lecture |
| POST | /api/v1/payment/capturePayment | Student | Start payment |
| POST | /api/v1/payment/verifyPayment | Student | Verify & enroll |
| GET | /api/v1/profile/getUserDetails | Auth | Profile |
| GET | /api/v1/profile/getEnrolledCourses | Auth | My courses |
| GET | /api/v1/profile/instructorDashboard | Instructor | Analytics |
| POST | /api/v1/ai/generate-summary | Auth | AI summary |
| GET | /api/v1/ai/recommendations | Student | AI recommend |
| POST | /api/v1/ai/chat/{courseId} | Auth | AI chatbot |
| POST | /api/v1/ai/generate-quiz | Auth | AI quiz |
