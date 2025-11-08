# Build & Development Commands

## Building the Project
```bash
# Clean and install all modules
mvn clean install

# Build Docker images for all services
mvn spring-boot:build-image

# Build specific service
cd services/asapp-authentication-service && mvn clean install
```

## Running Tests
```bash
# Run all tests (unit + integration)
mvn test verify

# Run only unit tests
mvn test

# Run only integration tests
mvn verify -DskipUnitTests

# Run tests for specific service
cd services/asapp-authentication-service && mvn test

# Run mutation testing (PITest)
mvn org.pitest:pitest-maven:mutationCoverage
```

## Code Quality & Formatting
```bash
# Check code style (uses Spotless)
mvn spotless:check

# Apply code formatting
mvn spotless:apply

# Install git hooks (automatic on mvn install)
mvn git-build-hook:install
```

## Database Management (Liquibase)
```bash
# Generate migration SQL (from project root)
cd services/asapp-authentication-service
mvn liquibase:updateSQL

# Clear checksums (if needed)
mvn liquibase:clearCheckSums

# Rollback last changeset
mvn liquibase:rollback -Dliquibase.rollbackCount=1
```

## Running the Application
```bash
# Start all services with Docker Compose
docker-compose up -d

# View logs
docker-compose logs -f asapp-authentication-service

# Stop and remove all services (with volumes)
docker-compose down -v

# Run single service locally (ensure database is available)
cd services/asapp-authentication-service
mvn spring-boot:run
```

## Accessing Services
- Authentication Service Swagger: http://localhost:8080/asapp-authentication-service/swagger-ui.html
- Users Service Swagger: http://localhost:8081/asapp-users-service/swagger-ui.html
- Tasks Service Swagger: http://localhost:8082/asapp-tasks-service/swagger-ui.html
- Grafana Dashboards: http://localhost:3000