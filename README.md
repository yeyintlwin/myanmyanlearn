# MYAN MYAN LEARN

A Spring Boot application designed for efficient JKen preparation with instant translation capabilities and comprehensive learning modules.

## Description

MYAN MYAN LEARN is designed for efficient JKen preparation. Documents are available with instant translation to your preferred language for rapid comprehension. The platform provides separate Learn and Test modules, including post-lesson quizzes and a comprehensive score chart for progress tracking.

## Features

- **Instant Translation**: Documents available with instant translation to your preferred language for rapid comprehension
- **Learn Module**: Dedicated learning section for JKen preparation
- **Test Module**: Comprehensive testing capabilities
- **Post-lesson Quizzes**: Interactive quizzes after each lesson
- **Score Chart**: Comprehensive progress tracking with score visualization
- **Multi-language Support**: Translation capabilities for various languages

## Technology Stack

- **Java 21**: Modern Java development
- **Spring Boot 3.5.6**: Enterprise-grade Java framework
- **Spring Web**: RESTful web services
- **Lombok**: Reduces boilerplate code
- **Gradle**: Build automation and dependency management
- **JUnit 5**: Testing framework

## Prerequisites

- Java 21 or higher
- Gradle 7.0 or higher

## Getting Started

### Installation

1. Clone the repository:

```bash
git clone <repository-url>
cd myanmyanlearn
```

2. Build the application:

```bash
./gradlew build
```

3. Run the application:

```bash
./gradlew bootRun
```

The application will be available at `http://localhost:8080`

### Development

For development with auto-reload:

```bash
./gradlew bootRun --continuous
```

### Testing

Run the test suite:

```bash
./gradlew test
```

## Project Structure

```
src/
├── main/
│   ├── java/com/barlarlar/myanmyanlearn/
│   │   └── MyanmyanlearnApplication.java
│   └── resources/
│       ├── application.properties
│       ├── static/
│       └── templates/
└── test/
    └── java/com/barlarlar/myanmyanlearn/
        └── MyanmyanlearnApplicationTests.java
```

## Configuration

The application can be configured through `src/main/resources/application.properties`:

```properties
spring.application.name=myanmyanlearn
```

## API Documentation

This application provides RESTful web services. For detailed API documentation, refer to the Spring Boot guides:

- [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
- [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
- [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)

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
