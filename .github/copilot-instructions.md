# Myan Myan Learn - AI Coding Agent Instructions

## Project Overview

**Myan Myan Learn** is a Spring Boot 3.5.6 application for JKen (Japanese language proficiency) preparation with multi-language support and comprehensive learning/testing modules. Uses Java 21, MySQL with JPA/Hibernate, Thymeleaf templating, and Spring Security with JWT for password resets.

## Architecture & Core Patterns

### Layered Architecture

- **Controllers** (`controller/`): Thymeleaf-based MVC views, handle form submissions and redirects
- **Services** (`service/`): Business logic layer with transactional boundaries
- **Entities** (`entity/`): JPA-mapped database models using `jakarta.persistence`
- **Repositories** (`repository/`): Spring Data JPA interfaces with custom queries
- **Models** (`model/`): Non-persistent DTOs for course structure (Course, Content, Subcontent)

### Database Schema

- **Members table**: User accounts with userId as PK, email unique constraint, OTP verification fields
- **Roles table**: Composite key (userId, role), uses JDBC-based Spring Security queries
- **OtpVerification & PasswordResetToken** entities: Support email verification and password reset flows
- See `src/main/resources/application.properties` for MySQL config (database: `user_directory`)

### Multi-Language System

- **i18n via cookies**: `WebConfig` uses `CookieLocaleResolver` with cookie name `selectedLanguage`, defaults to English
- **Message properties**: `messages.properties` + language-specific variants (`messages_my.properties`, `messages_zh.properties`, etc.)
- Message source configured with UTF-8 encoding and system locale fallback disabled

### Course Content System

- **JSON-based course loading**: CourseService uses lazy singleton pattern with synchronized caching
- **ClassPath resolution**: Courses loaded from `classpath:/courses/*/course.json` at startup
- **Course models**: Course → Contents (chapters) → Subcontents (lessons), all non-persistent POJOs
- **Static asset serving**: `WebConfig` maps `/courses/**` to classpath resources

## Security & Authentication

### Spring Security Configuration

- **UserDetails source**: JDBC-based (queries in `SecurityConfig.setUsersByUsernameQuery()`)
- **Authentication flow**: Form login at `/login` → `/authenticateTheUser` POST
- **Remember-me**: 30-day token validity, configured in `SecurityConfig`
- **Session management**: Single session per user (prevents concurrent login)
- **Public routes**: Auth-exempt paths like `/register`, `/forget-password`, `/language` explicitly listed in `permitAll()`

### Email Verification & Password Reset

- **OTP flow**: Generated in `RegistrationService.registerUser()`, 10-minute expiry, sent via `EmailService`
- **JWT tokens**: `JwtService` generates password reset tokens with 60-minute default expiry (configurable via `app.jwt.reset.expiry-minutes`)
- **SMTP config**: Gmail SMTP with app-specific password in properties file

### Password Encoding

- BCrypt via `passwordEncoder()` bean in `SecurityConfig`
- All password comparisons happen in Spring Security filter chain

## Key Service Patterns

### RegistrationService

- Validates unique userId and email before creating members
- Assigns default `ROLE_EMPLOYEE` on registration
- Generates OTP with `LocalDateTime` expiry, clearing OTP after verification
- Email verification required before full account activation

### EmailService

- Sends OTP and password reset emails via JavaMailSender
- Uses simple `SimpleMailMessage` (not MIME for now)
- Logs failures to stderr for debugging

### JwtService

- Uses JJWT library (version 0.11.5) with HS256 signature algorithm
- Reset tokens stored in `PasswordResetToken` entity (separate from member)
- Subject field set to "password-reset", email in claims

### CourseService

- Thread-safe lazy loading with double-checked locking pattern
- ObjectMapper parses course.json files into Course POJOs
- Case-insensitive search by id and title
- Returns null on invalid lookups (not exceptions)

## Common Workflows

### Build & Run

```bash
./gradlew build                    # Full build with tests
./gradlew bootRun                  # Run application (port 8080)
./gradlew bootRun --continuous    # Watch mode
./gradlew test                     # Run tests with JUnit5
```

### Database Initialization

- Uses Hibernate `ddl-auto=update` (auto-creates/updates schema)
- SQL backup script: `scripts/mysql_backup_secure.sh`
- Default credentials in `application.properties` (development only)

### Development Patterns

- Templates in `src/main/resources/templates/` use Thymeleaf syntax
- Redirect prefer `RedirectAttributes` for flash attributes
- Model attributes passed to templates in controller methods
- Controllers return template names (string) not views directly

## Important Conventions

1. **User identification**: `userId` (username) used as primary key; `email` used for verification/reset flows
2. **Null safety**: CourseService returns null on not-found; other services throw `RuntimeException`
3. **Transaction scope**: `@Transactional` on services enables lazy-loading of related entities
4. **Request mapping**: Form submissions use `@PostMapping` with explicit URL (not just `/path`)
5. **Lombok usage**: `@Entity` classes use Lombok; generated equals/hashCode based on id fields
6. **Error handling**: Login failures redirect with `?error=true` query param; controller checks for param

## Cross-Component Communication

- **Controllers → Services**: Dependency injection via `@Autowired`
- **Services → Repositories**: Custom finder methods in `MemberRepository` (`findByEmail()`, `existsByUserId()`)
- **Services → External**: EmailService calls JavaMailSender; JwtService cryptographic operations
- **Templates ← Model**: Thymeleaf expressions access model attributes; `Locale` available via `org.springframework.web.servlet.LocaleContextHolder`

## Testing & Validation

- Test structure: `src/test/java/com/barlarlar/myanmyanlearn/`
- Default test: `MyanmyanlearnApplicationTests.java`
- Test runner: JUnit 5 Platform
- No current integration/component tests; focus on unit tests for new features

---

**File reference for patterns**:

- Security: `src/main/java/com/barlarlar/myanmyanlearn/security/SecurityConfig.java`
- Multi-lang: `src/main/java/com/barlarlar/myanmyanlearn/config/WebConfig.java`
- Auth services: `src/main/java/com/barlarlar/myanmyanlearn/service/{RegistrationService,JwtService}.java`
- Core entity: `src/main/java/com/barlarlar/myanmyanlearn/entity/Member.java`
