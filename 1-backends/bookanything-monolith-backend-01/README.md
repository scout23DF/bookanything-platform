# BookAnything Platform - Monolith Backend

![Java](https://img.shields.io/badge/Java-17-blue.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25-blueviolet.svg)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.3-brightgreen.svg)
![Kafka](https://img.shields.io/badge/Apache_Kafka-2.x-lightgrey.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)
![Elasticsearch](https://img.shields.io/badge/Elasticsearch-8.x-005571.svg)
![Docker](https://img.shields.io/badge/Docker-24.x-blue.svg)

## Overview

This project is the first core backend service for the **BookAnything Platform**. It's a Kotlin Spring Boot application designed to manage and locate any kind of bookable/reservable entities that have a physical location. It implements a hexagonal architecture and the Command Query Responsibility Segregation (CQRS) pattern, leveraging Apache Kafka for asynchronous communication and data consistency.

This service is part of a larger monorepo, the **BookAnything-Platform**, which aims to provide a complete solution for booking and acquiring products or services.

## Features

*   **Localizable Entity Management:** RESTful API for creating, retrieving, updating, and deleting records of entities with geographic locations.
*   **Geo-spatial Queries:** Efficiently find entities within a specified radius using Elasticsearch and PostGIS.
*   **Asynchronous GeoJSON Processing:** Upload GeoJSON files containing multiple entities, which are then processed asynchronously via Kafka.
*   **Data Synchronization:** Manual synchronization endpoint to ensure data consistency between PostgreSQL (write-model) and Elasticsearch (read-model).
*   **Optimized Bulk Operations:** Efficient bulk deletion of entities, leveraging Kafka events and Elasticsearch's `deleteAll` functionality.
*   **Event-Driven Architecture:** Utilizes Kafka for event publishing to maintain data consistency.
*   **API Documentation:** Integrated Swagger UI for interactive API exploration.

## Architecture

The project adheres to a **Hexagonal Architecture**, separating the core domain logic from external concerns (databases, messaging, APIs). It also implements **CQRS**, with PostgreSQL serving as the write-model (source of truth) and Elasticsearch as the read-model, optimized for complex queries. Kafka acts as the central nervous system for asynchronous communication and eventual consistency.

## Technologies Used

*   **Language:** Kotlin
*   **Framework:** Spring Boot (3.5.3)
*   **Build Tool:** Maven
*   **Database:** PostgreSQL with PostGIS extension
*   **Search Engine:** Elasticsearch (8.x)
*   **Messaging:** Apache Kafka (7.6.0)
*   **Authentication/Authorization:** Spring Security (OAuth2 Resource Server, integrated with Keycloak)
*   **Geo-spatial Libraries:**
    *   JTS (Java Topology Suite)
    *   `geojson-jackson` & `jackson-datatype-jts`
*   **API Documentation:** Springdoc OpenAPI (Swagger UI)
*   **Containerization:** Docker, Docker Compose

## Getting Started

### Prerequisites

*   Java Development Kit (JDK) 17+
*   Apache Maven (3.x+)
*   Docker & Docker Compose

### Cloning the Repository

```bash
git clone https://github.com/YOUR_USERNAME/bookanything-platform.git
cd bookanything-platform/1-backends/bookanything-monolith-backend-01
```

### Running Dependent Services with Docker Compose

Navigate to the `1-backends/bookanything-monolith-backend-01` directory and run:

```bash
docker-compose up -d
```

This will start all required services (PostgreSQL, Kafka, Elasticsearch, etc.).

### Building and Running the Spring Boot Application

```bash
# Build the project
./mvnw clean install

# Run the application
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`.

## API Endpoints

The API documentation is available via Swagger UI at `http://localhost:8080/swagger-ui.html`.

Key endpoints include:

*   `POST /api/v1/localizable-places`: Create a new localizable entity.
*   `GET /api/v1/localizable-places/{id}`: Retrieve an entity by ID.
*   `GET /api/v1/localizable-places/all`: Retrieve all entities.
*   `GET /api/v1/localizable-places/search-nearby`: Find entities within a given radius.
*   `DELETE /api/v1/localizable-places/{id}`: Delete an entity by ID.
*   `DELETE /api/v1/localizable-places/all`: Delete all entities.
*   `POST /api/v1/localizable-places/synchronize`: Trigger manual data synchronization.
*   `POST /api/v1/localizable-places/upload-geojson`: Upload a GeoJSON file for asynchronous processing.

## Contributing

Contributions are welcome! Please feel free to fork the repository, open issues, or submit pull requests.

## License

This project is licensed under the MIT License.