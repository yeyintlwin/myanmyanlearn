# MYAN MYAN LEARN

A Spring Boot learning platform for international students in Japan, designed to support multiple school subjects with instant translation into the learner’s preferred language.

## Features

- **Instant Translation**: Documents available with instant translation to your preferred language for rapid comprehension
- **Learn Module**: Dedicated learning section for course/subject content
- **Test Module**: Comprehensive testing capabilities
- **Post-lesson Quizzes**: Interactive quizzes after each lesson
- **Score Chart**: Comprehensive progress tracking with score visualization
- **Multi-language Support**: Translation capabilities for various languages

## Prerequisites

- Java 21
- MySQL 8.x

## Getting Started

### 1) Configure the database

- Create the database:

```bash
mysql -u root -p -e "CREATE DATABASE user_directory DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_0900_ai_ci;"
```

- First run schema bootstrap:
  - If the database has no tables and `spring.jpa.hibernate.ddl-auto` is `validate` (default), the app will automatically create the tables on startup.
  - After the tables exist, it will keep using `validate`.

- Optional (restore schema-only dump):
  - The schema SQL is in [scripts/backups](scripts/backups).

```bash
mysql -u root -p user_directory < scripts/backups/user_directory_20260109_195828.sql
```

### 2) Configure application.properties

Copy the example config and fill in values:

```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

Required properties:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/user_directory
spring.datasource.username=<db_username>
spring.datasource.password=<db_password>
app.jwt.reset.secret=<replace_with_256bit_secret>
```

Optional (enables translation in Reader):

```properties
google.studio.api-key=<your_api_key>
```

### 3) Run

1. Clone the repository:

```bash
git clone <repository-url>
cd myanmyanlearn
```

```bash
./gradlew bootRun
```

The application will be available at `http://localhost:8080`

### Testing

Run the test suite:

```bash
./gradlew test
```

## Course export/import (.bll)

- Export (Admin): open the Courses admin page and choose Export on a course to download a `.bll` archive.
- Import (Admin): use Import on the Courses admin page and upload a `.bll` archive to restore a course.
- What’s inside: course editor JSON + uploaded assets under `courses/<courseId>/` (cover image included).
- Limits: max upload size is 25MB.
- Delete behavior: deleting a course also deletes its stored assets under `courses/<courseId>/`.

## Storage

- Default (local): stored at `${user.dir}/.myanmyanlearn/uploads` and served from `/uploads/**`.
- S3: set `app.storage.type=s3` and configure:

```properties
app.storage.s3.bucket=<bucket>
app.storage.s3.region=<region>
app.storage.s3.key-prefix=<optional_prefix>
app.storage.s3.public-url-prefix=<optional_public_url_prefix>
app.storage.s3.public-read=true
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support and questions, please open an issue in the repository.

## References

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/3.5.6/reference/htmlsingle/)
- [Gradle Documentation](https://docs.gradle.org)
- [Spring Web Documentation](https://docs.spring.io/spring-boot/3.5.6/reference/web/servlet.html)
