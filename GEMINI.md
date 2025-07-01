# Project Context: Distribution Center Locator Microservice

This document provides a comprehensive overview of the `distribution-center-locator` microservice, detailing its architecture, features, and current state.

## Overview

The `distribution-center-locator` is a Kotlin Spring Boot microservice designed to manage distribution centers. It leverages a hexagonal architecture to ensure clear separation of concerns and maintainability.

## Key Features and Functionality

*   **REST API:** Provides a comprehensive set of RESTful endpoints for managing distribution centers, including:
    *   **Create:** Register new distribution centers.
    *   **Read:** Retrieve distribution center information by ID, list all centers, and find centers within a specified radius.
    *   **Delete:** Remove individual distribution centers by ID or perform bulk deletions of all centers.
    *   **Synchronize:** A dedicated endpoint (`/synchronize`) to trigger a full data synchronization from the primary data store (PostgreSQL) to the query data store (Elasticsearch).
    *   **GeoJSON Upload:** An endpoint (`/upload-geojson`) to asynchronously process GeoJSON files containing distribution center data.

*   **Asynchronous Processing with Kafka:**
    *   **GeoJSON File Uploads:** GeoJSON content is published as `GeoJsonUploadedFileDTO` messages to a Kafka topic (`geojson-upload-topic`).
    *   **Kafka Producer Configuration:** Configured to handle large messages (50MB `max.request.size`) and correctly serialize custom DTOs using `JsonSerializer`.
    *   **Kafka Consumer Configuration:** Configured to receive large messages (50MB `max.partition.fetch.bytes`) and trust custom DTO packages (`br.com.geminiproject.dcl.domain.geojson`) for deserialization.
    *   **`GeoJsonKafkaConsumer`:** A dedicated Kafka consumer processes GeoJSON messages, parsing the content and registering distribution centers.
    *   **Event-Driven Consistency:** Kafka is used to ensure data consistency between the write and query repositories for create and delete operations.

*   **CQRS (Command Query Responsibility Segregation) Pattern:**
    *   **Write Repository (PostgreSQL/JPA):**
        *   Serves as the primary data store and source of truth for transactional data.
        *   Enforces data integrity with a unique constraint on the `nome` field for distribution centers.
        *   Includes a `existsByName` method with `@Cacheable` annotation for performance optimization of existence checks.
    *   **Query Repository (Elasticsearch):**
        *   Optimized for read operations, particularly geo-spatial queries (e.g., finding centers within a radius).
        *   Maintains consistency with the write repository through Kafka events and a synchronization mechanism.

*   **Data Synchronization:**
    *   The `/synchronize` endpoint triggers a process that reads all distribution center data from PostgreSQL and re-indexes it into Elasticsearch, ensuring eventual consistency.
    *   Bulk deletion of distribution centers in PostgreSQL triggers a single Kafka event (`CentrosDistribuicaoDeletadosEvent`), which is consumed by a dedicated Kafka consumer to perform a `deleteAll()` operation in Elasticsearch, significantly improving performance for mass deletions.

*   **Domain Model:**
    *   `CentroDistribuicaoModel`: The core domain object representing a distribution center.
    *   `GeoJsonUploadedFileDTO`: A data transfer object used for Kafka messages containing GeoJSON file content.

*   **Architectural Principles:**
    *   **Hexagonal Architecture:** Promotes loose coupling and testability by separating core domain logic from external concerns (databases, messaging, APIs).
    *   **Event-Driven Architecture:** Utilizes Kafka for asynchronous communication and to maintain data consistency across different data stores.

*   **Technology Stack:**
    *   **Language:** Kotlin
    *   **Framework:** Spring Boot
    *   **Build Tool:** Maven
    *   **Database:** PostgreSQL with PostGIS extension (for spatial data)
    *   **Search Engine:** Elasticsearch
    *   **Messaging:** Apache Kafka
    *   **Authentication/Authorization:** Spring Security with OAuth2 Resource Server (Keycloak integration)
    *   **API Documentation:** Springdoc OpenAPI (Swagger UI)
    *   **Monitoring:** Spring Boot Actuator
    *   **Geo-spatial Libraries:** JTS (Java Topology Suite) for geometric operations, `geojson-jackson` and `jackson-datatype-jts` for GeoJSON serialization/deserialization.

## Current State

The project is in a robust state, with a well-defined architecture and implemented features for managing distribution centers. Recent enhancements include:

*   Asynchronous GeoJSON file processing via Kafka.
*   Improved handling of large GeoJSON file uploads.
*   Optimized bulk deletion of distribution centers in Elasticsearch.
*   Implementation of a unique name constraint for distribution centers with caching for existence checks.

The system is designed for scalability, consistency, and maintainability, leveraging modern microservice patterns and technologies.
