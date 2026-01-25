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

- The schema SQL is in [scripts/backups](scripts/backups) (schema-only dump).

```bash
mysql -u root -p -e "CREATE DATABASE user_directory DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_0900_ai_ci;"
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
