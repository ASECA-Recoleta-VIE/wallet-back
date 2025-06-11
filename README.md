# Wallet API Backend

This repository contains the backend API for the Wallet application.

## Running the Application

There are two ways to run the application:

### 1. Using Docker (Recommended for Production)

The application can be run using Docker Compose, which will set up both the application and the PostgreSQL database:

```bash
docker-compose up -d
```

This will start:
- PostgreSQL database on port 5432
- Wallet API on port 8080

### 2. Local Development (Without Docker)

For local development without Docker, you can use the built-in H2 database:

```bash
# Run with dev profile (uses H2 in-memory database)
./gradlew bootRun --args='--spring.profiles.active=dev'
```

With the dev profile:
- The application uses an H2 in-memory database
- H2 console is available at http://localhost:8080/h2-console
- Database credentials: username=sa, password=password
- JDBC URL: jdbc:h2:mem:devdb

## API Documentation

When the application is running, you can access the API documentation at:
- http://localhost:8080/swagger-ui.html

## Database Configuration

### PostgreSQL (Production)
- Database: walletdb
- Username: walletuser
- Password: walletpassword
- Port: 5432

### H2 (Development)
- In-memory database
- Console: http://localhost:8080/h2-console
- JDBC URL: jdbc:h2:mem:devdb
- Username: sa
- Password: password
