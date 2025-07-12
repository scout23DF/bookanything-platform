# Distribution Center Locator Microservice

![Java](https://img.shields.io/badge/Java-17-blue.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25-blueviolet.svg)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.3-brightgreen.svg)
![Kafka](https://img.shields.io/badge/Apache_Kafka-2.x-lightgrey.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)
![Elasticsearch](https://img.shields.io/badge/Elasticsearch-8.x-005571.svg)
![Docker](https://img.shields.io/badge/Docker-24.x-blue.svg)

## Overview

This project is a Kotlin Spring Boot microservice designed to efficiently manage and locate distribution centers. It implements a hexagonal architecture and the Command Query Responsibility Segregation (CQRS) pattern, leveraging Apache Kafka for asynchronous communication and data consistency across different data stores.

## Features

*   **Distribution Center Management:** RESTful API for creating, retrieving, updating, and deleting distribution center records.
*   **Geo-spatial Queries:** Efficiently find distribution centers within a specified radius using Elasticsearch and PostGIS.
*   **Asynchronous GeoJSON Processing:** Upload GeoJSON files containing multiple distribution centers, which are then processed asynchronously via Kafka.
*   **Data Synchronization:** Manual synchronization endpoint to ensure data consistency between PostgreSQL (write-model) and Elasticsearch (read-model).
*   **Optimized Bulk Operations:** Efficient bulk deletion of distribution centers, leveraging Kafka events and Elasticsearch's `deleteAll` functionality.
*   **Unique Name Constraint:** Prevents duplicate distribution center names with database-level enforcement and cached existence checks for performance.
*   **Event-Driven Architecture:** Utilizes Kafka for event publishing (e.g., on creation, individual deletion, bulk deletion) to maintain data consistency.
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
    *   JTS (Java Topology Suite) for geometric operations.
    *   `geojson-jackson` (for GeoJSON structure mapping).
    *   `jackson-datatype-jts` (for JTS geometry serialization/deserialization).
*   **API Documentation:** Springdoc OpenAPI (Swagger UI)
*   **Containerization:** Docker, Docker Compose

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

Before you begin, ensure you have the following installed:

*   Java Development Kit (JDK) 17 or higher
*   Apache Maven (3.x or higher)
*   Docker Desktop (or Docker Engine and Docker Compose)

### Cloning the Repository

```bash
git clone https://github.com/YOUR_USERNAME/distribution-center-locator.git
cd distribution-center-locator
```

### Running Dependent Services with Docker Compose

This project relies on PostgreSQL (with PostGIS), Apache Kafka, Zookeeper, Elasticsearch, Kibana, and Keycloak. All these services can be started using Docker Compose.

Navigate to the root directory of the project where `docker-compose.yml` is located and run:

```bash
docker-compose up -d
```

This command will download the necessary Docker images and start all services in detached mode. It might take a few minutes for all services to be fully up and running.

### Building and Running the Spring Boot Application

Once the Docker services are up, you can build and run the Spring Boot application.

```bash
# Build the project
./mvnw clean install

# Run the application
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`.

## API Endpoints

The API documentation is available via Swagger UI at `http://localhost:8080/swagger-ui.html`.

Here's a summary of the main endpoints:

*   `POST /cds`: Create a new distribution center.
*   `GET /cds/{id}`: Retrieve a distribution center by ID.
*   `GET /cds/all`: Retrieve all distribution centers.
*   `GET /cds/search-nearby`: Find distribution centers within a given radius.
    *   Parameters: `latitude`, `longitude`, `raioEmKm`.
*   `DELETE /cds/{id}`: Delete a distribution center by ID.
*   `DELETE /cds/all`: Delete all distribution centers (optimized bulk deletion).
*   `POST /cds/synchronize`: Trigger a manual synchronization of data from PostgreSQL to Elasticsearch.
*   `POST /cds/upload-geojson`: Upload a GeoJSON file for asynchronous processing.
    *   Consumes `multipart/form-data`.
    *   Parameters: `contentDataType` (String), `file` (MultipartFile).

## Contributing

Feel free to fork the repository, open issues, or submit pull requests. Any contributions are welcome!

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details. (Note: You might need to create a LICENSE file in your repository if you don't have one.)

