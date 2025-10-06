# oportunidade-backend

Quarkus-based backend for Oportunidade platform with AppyPay payment webhook integration.

## Features

- **Payment Reference Management**: Create and manage payment references for MultiCaixa ATMs
- **AppyPay Webhook Integration**: Receive and process payment notifications asynchronously
- **Idempotency**: Prevent duplicate webhook processing
- **Comprehensive Testing**: Unit and integration tests with Testcontainers
- **Health Monitoring**: Health checks and Prometheus metrics
- **Database Migrations**: Flyway for version-controlled schema

## Quick Start

### Prerequisites

- Java 17+
- PostgreSQL 12+
- Gradle 7+

### Setup

1. **Clone the repository**
```bash
git clone https://github.com/ovandunen/oportunidade-backend.git
cd oportunidade-backend
```

2. **Configure database**
```bash
export DB_URL=jdbc:postgresql://localhost:5432/oportunidade
export DB_USERNAME=postgres
export DB_PASSWORD=your_password
```

3. **Run in development mode**
```bash
./gradlew quarkusDev
```

The application will be available at `http://localhost:8080`

### API Endpoints

- **Webhooks**: `POST /webhooks/appypay` - Receive AppyPay payment notifications
- **Health**: `GET /webhooks/appypay/health` - Service health check
- **API Docs**: `http://localhost:8080/q/swagger-ui/` - OpenAPI documentation
- **Health UI**: `http://localhost:8080/q/health-ui/` - Health check UI
- **Metrics**: `http://localhost:8080/q/metrics` - Prometheus metrics

## Documentation

- **[Implementation Summary](IMPLEMENTATION_SUMMARY.md)** - Overview of what was built
- **[AppyPay Integration Guide](docs/APPYPAY_WEBHOOK_INTEGRATION.md)** - Detailed integration documentation
- **[Webhook Examples](docs/WEBHOOK_EXAMPLES.md)** - Example payloads and cURL commands
- **[Troubleshooting Guide](docs/TROUBLESHOOTING.md)** - Common issues and solutions

## Testing

Run all tests:
```bash
./gradlew test
```

Run with coverage:
```bash
./gradlew test jacocoTestReport
```

## Tech Stack

- **Framework**: Quarkus 3.8.6
- **Language**: Java 17
- **Database**: PostgreSQL with Hibernate Panache
- **Async**: SmallRye Reactive Messaging
- **Testing**: JUnit 5, RestAssured, Testcontainers
- **Build**: Gradle
- **Utilities**: Lombok, MapStruct

## Project Structure

```
src/
├── main/
│   ├── java/ao/co/oportunidade/
│   │   ├── webhook/          # AppyPay webhook integration
│   │   │   ├── dto/          # Data transfer objects
│   │   │   ├── entity/       # Domain entities and repositories
│   │   │   ├── service/      # Business logic
│   │   │   ├── resource/     # REST endpoints
│   │   │   └── health/       # Health checks
│   │   └── ...               # Existing code
│   └── resources/
│       ├── application.properties
│       └── db/migration/     # Flyway migrations
└── test/
    ├── java/...              # Unit and integration tests
    └── resources/
```

## License

[Your License]