# Project Context: `bookanything-monolith-backend-01`

This document provides a comprehensive overview of the `bookanything-monolith-backend-01` service, part of the larger `BookAnything-Platform`.

## 1. Overview

`bookanything-monolith-backend-01` is a Spring Boot application written in Kotlin that serves as the foundational backend for the BookAnything platform. It is designed as a modular monolith, with clear separation between different business domains. The service manages entities that have a geographical component ("localizable places") and assets related to them.

The project is structured as a Maven project and is the primary component currently under development within the `1-backends` module of the monorepo.

## 2. Architecture and Key Features

The service leverages a modern, event-driven architecture and incorporates several key patterns and technologies:

*   **Modular Monolith:** The code is organized into distinct domain packages (`dom01geolocation`, `dom02assetmanager`) to promote separation of concerns and facilitate a potential future migration to microservices.
*   **CQRS (Command Query Responsibility Segregation):**
    *   **Write Operations:** Handled via standard JPA repositories, with PostgreSQL (and PostGIS for spatial data) serving as the source of truth.
    *   **Read Operations:** Complex queries, especially geo-spatial searches, are offloaded to Elasticsearch for high performance.
*   **Event-Driven Architecture:** Apache Kafka is used for asynchronous communication and to ensure data consistency between the PostgreSQL and Elasticsearch data stores. Events are published for significant state changes (e.g., `LocalizablePlaceCreatedEvent`).
*   **REST API:** A comprehensive RESTful API is exposed for managing all application entities. API documentation is generated and available via SpringDoc (Swagger UI).
*   **AI Integration:** The application is integrated with Google's Vertex AI (Gemini Pro) for advanced functionalities, as seen in the `GetGeoLocationBoundaryViaAIService`.
*   **File/Object Storage:** Minio is used for S3-compatible object storage, for instance, to manage GeoJSON file uploads and downloads.
*   **Security:** The application is secured using Spring Security with OAuth2/JWT, delegating authentication and authorization to Keycloak.

## 3. Technology Stack

*   **Language:** Kotlin
*   **Framework:** Spring Boot 3
*   **Build Tool:** Maven
*   **Primary Database:** PostgreSQL with PostGIS extension for geo-spatial data.
*   **Database Migrations:** Liquibase
*   **Read/Search Database:** Elasticsearch
*   **Messaging Broker:** Apache Kafka
*   **Identity and Access Management (IAM):** Keycloak
*   **AI Provider:** Google Vertex AI (Gemini)
*   **Object Storage:** Minio
*   **API Documentation:** SpringDoc (OpenAPI 3)
*   **Testing:** JUnit 5, MockK, Testcontainers (for integration tests with Kafka, PostgreSQL, etc.)

## 4. Development Environment

The local development environment is fully containerized using **Docker Compose**. The `docker-compose.yml` file orchestrates all necessary backing services, including:
*   Portainer (Container Management)
*   Minio (Object Storage)
*   PostgreSQL & PgAdmin
*   Kafka & Zookeeper
*   RabbitMQ
*   Keycloak (IAM)
*   Consul (Service Discovery/Configuration)
*   Elasticsearch & Kibana

This setup ensures a consistent and reproducible development environment for all team members.

## 5. Core Business Domains

*   **`dom01geolocation`:** Manages all aspects of localizable places.
    *   CRUD operations for `LocalizablePlace` and `Address` entities.
    *   Geo-spatial queries (e.g., find places within a radius).
    *   Asynchronous processing of GeoJSON file uploads via Kafka for data import.
*   **`dom02assetmanager`:** Manages assets that can be associated with localizable places. (Details to be expanded).
