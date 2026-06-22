# InternIQ Spring Boot Backend

This Spring Boot backend supports InternIQ authentication, intern management, attendance, tasks, feedback, candidates, AI resume screening, and mocked AI interviews.

## Created Modules

- `auth`: register, login, and current-user APIs
- `config`: Spring Security, JWT filter, and CORS configuration
- `security`: JWT token service and custom user loading
- `user`: user entity, role enum, repository, service, and protected user endpoint
- `department`: department CRUD with HR/admin write access
- `intern`: intern profile management and role-aware visibility
- `attendance`: intern punch-in/punch-out and attendance history
- `task`: manager task assignment, intern submission, and manager review
- `feedback`: manager feedback and role-aware feedback views
- `candidate`: HR/admin candidate creation and listing
- `ai`: provider-ready resume screening and interview evaluation with local mock default
- `resume`: local resume file storage and text extraction
- `interview`: mocked AI interview scheduling, questions, answers, and results
- `common`: shared API response wrapper and exception handling

## Create MySQL Database

```sql
CREATE DATABASE interniq_db;
```

Optional dedicated user:

```sql
CREATE USER 'interniq_user'@'localhost' IDENTIFIED BY 'strong_password';
GRANT ALL PRIVILEGES ON interniq_db.* TO 'interniq_user'@'localhost';
FLUSH PRIVILEGES;
```

## Configure Environment Variables

The app has safe development defaults, but these variables should be set locally:

```bash
export DB_USERNAME=interniq_user
export DB_PASSWORD=strong_password
export JWT_SECRET=replace-with-a-long-random-secret-of-at-least-32-characters
```

AI resume screening and AI interviews work without an API key by using local mock analysis. This is the default:

```bash
export AI_PROVIDER=mock
export AI_API_KEY=
export AI_BASE_URL=https://api.openai.com
export AI_MODEL=gpt-4o-mini
export RESUME_UPLOAD_DIR=uploads/resumes
```

For a future OpenAI-compatible provider, set:

```bash
export AI_PROVIDER=openai-compatible
export AI_API_KEY=your-provider-api-key
export AI_BASE_URL=https://api.openai.com
export AI_MODEL=gpt-4o-mini
export RESUME_UPLOAD_DIR=uploads/resumes
```

If `AI_PROVIDER=openai-compatible` is selected without `AI_API_KEY`, the backend fails safely with a clear configuration error. Do not commit API keys. Keep them in your shell, `.env`, deployment secret manager, or CI secrets.

Email notifications are mocked by default:

```bash
export EMAIL_PROVIDER=mock
```

To send real email through SMTP, set:

```bash
export EMAIL_PROVIDER=smtp
export MAIL_ENABLED=true
export MAIL_FROM=no-reply@your-domain.com
export EMAIL_APP_URL=http://localhost:5173
export MAIL_HOST=smtp.gmail.com
export MAIL_PORT=587
export MAIL_USERNAME=your-smtp-user
export MAIL_PASSWORD=your-smtp-app-password
export SMTP_AUTH=true
export SMTP_STARTTLS=true
```

Use an SMTP sandbox such as Mailtrap or MailHog before sending to real inboxes. Never commit SMTP passwords.

Set `DB_USERNAME` and `DB_PASSWORD` in your local shell or deployment environment for MySQL access.

## Frontend Environment

The existing React app can stay as-is. To force real backend APIs, set:

```bash
VITE_USE_MOCK_API=false
VITE_ENABLE_MOCK_FALLBACK=false
VITE_API_BASE_URL=http://localhost:8080/api
```

CORS is enabled for:

```text
http://localhost:5173
http://127.0.0.1:5173
http://localhost:5178
http://127.0.0.1:5178
```

## Run Backend

```bash
cd spring-backend
mvn spring-boot:run
```

The backend runs on:

```text
http://localhost:8080
```

On first startup, Hibernate creates or updates the tables, and `CommandLineRunner` seeds departments, one intern profile, a sample task, a sample candidate, a sample interview, and these test users:

| Role | Email | Password |
| --- | --- | --- |
| INTERN | intern@test.com | 123456 |
| MANAGER | manager@test.com | 123456 |
| HR | hr@test.com | 123456 |
| ADMIN | admin@test.com | 123456 |

## Test Login in Postman

Send:

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json
```

Body:

```json
{
  "email": "intern@test.com",
  "password": "123456"
}
```

Copy `data.token` from the response.

Then call:

```http
GET http://localhost:8080/api/auth/me
Authorization: Bearer YOUR_TOKEN_HERE
```

## Frontend Login Call

The React app should call:

```js
const response = await axios.post("http://localhost:8080/api/auth/login", {
  email,
  password,
});

const { token, user } = response.data.data;
localStorage.setItem("interniq_token", token);
```

## Database Relationships

- `users` remains the authentication table.
- `departments` stores department master data.
- `intern_profiles.user_id` is a one-to-one link to `users.id` for users with the `INTERN` role.
- `intern_profiles.department_id` points to `departments.id`.
- `intern_profiles.manager_id` points to `users.id` for users with the `MANAGER` role.
- `attendance.intern_id` points to `intern_profiles.id`; each intern can have only one attendance row per date.
- `tasks.assigned_to_intern_id` points to `intern_profiles.id`.
- `tasks.assigned_by_user_id` points to the manager in `users.id`.
- `feedback.intern_id` points to `intern_profiles.id`.
- `feedback.manager_id` points to the manager in `users.id`.
- `feedback.task_id` optionally points to `tasks.id`.
- `candidates` stores candidate pipeline records.
- `resume_files.candidate_id` points to `candidates.id`.
- `resume_screening_results.candidate_id` stores the latest structured AI screening result.
- `interviews.candidate_id` optionally points to `candidates.id`.
- `interviews.intern_id` optionally points to `intern_profiles.id`.
- `interview_questions`, `interview_answers`, and `interview_results` point to `interviews.id`.

## API Summary

All APIs below require:

```http
Authorization: Bearer YOUR_TOKEN_HERE
```

Departments:

```http
GET    /api/users
GET    /api/users/role/{role}
GET    /api/departments
POST   /api/departments
PUT    /api/departments/{id}
DELETE /api/departments/{id}
```

Intern Profiles:

```http
GET  /api/interns
GET  /api/interns/{id}
POST /api/interns
PUT  /api/interns/{id}
GET  /api/interns/my-profile
```

Attendance:

```http
POST /api/attendance/punch-in
POST /api/attendance/punch-out
GET  /api/attendance/my
GET  /api/attendance/intern/{internId}
GET  /api/attendance/all
```

Tasks:

```http
POST /api/tasks
GET  /api/tasks/my
GET  /api/tasks/assigned-by-me
GET  /api/tasks/{id}
PUT  /api/tasks/{id}/status
PUT  /api/tasks/{id}/submit
PUT  /api/tasks/{id}/review
```

Feedback:

```http
POST /api/feedback
GET  /api/feedback/my
GET  /api/feedback/intern/{internId}
```

Candidates:

```http
GET  /api/candidates
GET  /api/candidates/{id}
POST /api/candidates
```

AI Resume Screening:

```http
POST /api/ai/resume-screen/{candidateId}
GET  /api/ai/resume-screen/{candidateId}
```

The `POST` endpoint expects multipart form data:

```text
file = resume.pdf | resume.doc | resume.docx | resume.txt
```

Interviews:

```http
POST /api/interviews
GET  /api/interviews/my
GET  /api/interviews/{id}
POST /api/interviews/{id}/generate-questions
POST /api/interviews/{id}/start
POST /api/interviews/{id}/answer
POST /api/interviews/{id}/complete
GET  /api/interviews/{id}/result
```

## Postman Testing Order

1. Login as HR or admin and copy the token.
2. Confirm seeded users with `GET /api/users`.
3. Confirm seeded departments with `GET /api/departments`.
4. Create a department if needed:

```json
{
  "name": "Engineering",
  "description": "Software engineering interns",
  "active": true
}
```

5. Confirm the seeded intern profile with `GET /api/interns`.
6. Create another intern profile if needed. Use IDs from API responses:

```json
{
  "userId": 1,
  "departmentId": 1,
  "managerId": 2,
  "phone": "9999999999",
  "college": "Demo University",
  "skills": "Java, React, SQL",
  "joiningDate": "2026-06-18",
  "internshipStartDate": "2026-06-18",
  "internshipEndDate": "2026-09-18",
  "status": "ACTIVE"
}
```

7. Login as manager and call `GET /api/interns` to confirm assigned interns.
8. Login as manager and assign a task:

```json
{
  "title": "Build login API integration",
  "description": "Connect React login form with Spring Boot auth API",
  "assignedToInternId": 1,
  "priority": "HIGH",
  "dueDate": "2026-06-25"
}
```

9. Login as intern and call `POST /api/attendance/punch-in`, then `POST /api/attendance/punch-out`.
10. Login as intern and call `GET /api/tasks/my`.
11. Update task status:

```json
{
  "status": "IN_PROGRESS"
}
```

12. Submit task work:

```json
{
  "submissionLink": "https://github.com/example/interniq-login",
  "submissionNote": "Implemented and tested login integration."
}
```

13. Login as manager and review the task:

```json
{
  "status": "APPROVED",
  "managerFeedback": "Good implementation and clear API handling.",
  "rating": 5
}
```

14. Login as manager and create feedback:

```json
{
  "internId": 1,
  "taskId": 1,
  "feedbackText": "Strong ownership and clean implementation.",
  "rating": 5
}
```

15. Login as HR/admin and create a candidate:

```json
{
  "name": "Riya Kapoor",
  "email": "riya@example.com",
  "phone": "8888888888",
  "appliedRole": "React Intern",
  "skills": "React, JavaScript, HTML, CSS"
}
```

16. Schedule an interview:

```json
{
  "candidateId": 1,
  "role": "React Intern",
  "scheduledAt": "2026-06-20T10:00:00"
}
```

17. Generate questions, start, submit answers, complete, then fetch result:

```http
POST /api/interviews/{id}/generate-questions
POST /api/interviews/{id}/start
POST /api/interviews/{id}/answer
POST /api/interviews/{id}/complete
GET  /api/interviews/{id}/result
```

## Frontend API Usage

- Admin/HR department screens use `/departments`.
- Admin/HR intern management screens use `/interns`.
- Manager intern screens use `/interns`.
- Intern profile page uses `/interns/my-profile`.
- Intern attendance punch card uses `/attendance/punch-in`, `/attendance/punch-out`, and `/attendance/my`.
- Manager attendance views use `/attendance/intern/{internId}` or `/attendance/all`.
- Intern tasks page uses `/tasks/my`, `/tasks/{id}/status`, and `/tasks/{id}/submit`.
- Manager task pages use `/tasks`, `/tasks/assigned-by-me`, and `/tasks/{id}/review`.
- Intern feedback uses `/feedback/my`.
- Manager, HR, and admin feedback views use `/feedback/intern/{internId}`.
- HR/admin candidate pages use `/candidates`.
- HR/admin interview pages use `/interviews`.
- Intern interview pages use `/interviews/my`, `/interviews/{id}`, `/interviews/{id}/answer`, and `/interviews/{id}/complete`.
# test-backend
