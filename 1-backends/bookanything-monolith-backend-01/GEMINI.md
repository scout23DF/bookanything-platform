# Project Context: BookAnything-Platform

This document provides a comprehensive overview of the `BookAnything-Platform` monorepo.

## Overview

The `BookAnything-Platform` is a monorepo that aggregates projects and assets for a complete solution. The platform's goal is to manage any product or service that can be scheduled/booked and subsequently acquired, following specific acquisition/hiring flows according to the product/service category.

The monorepo is structured as follows:
- `1-backends`: Contains backend services.
- `2-bff`: Contains Backend-for-Frontend services.
- `3-frontends`: Contains frontend applications.
- `4-clients-and-utilities`: Contains client libraries and various utilities.

## Current State: `bookanything-monolith-backend-01`

The main component currently under development is `bookanything-monolith-backend-01`, located in the `1-backends` directory. This is a Kotlin Spring Boot service that serves as the foundation for the platform.

### Key Features and Functionality of `bookanything-monolith-backend-01`

*   **Core Service:** Initially conceived as a `distribution-center-locator`, this service is being refactored to become the core backend for the BookAnything platform. It manages entities that are "localizable" or have a geographical component.
*   **REST API:** Provides a comprehensive set of RESTful endpoints for managing these localizable entities. This includes CRUD operations, geo-spatial queries (finding entities within a radius), and data synchronization.
*   **Asynchronous Processing with Kafka:** Uses Kafka for asynchronous tasks, such as processing uploaded GeoJSON files containing entity data. This ensures the platform is scalable and resilient.
*   **CQRS (Command Query Responsibility Segregation) Pattern:**
    *   **Write Repository (PostgreSQL/JPA):** The primary data store (source of truth).
    *   **Query Repository (Elasticsearch):** Optimized for complex read operations, especially geo-spatial queries.
*   **Event-Driven Architecture:** Kafka is used to maintain data consistency between the write (PostgreSQL) and read (Elasticsearch) repositories.
*   **Technology Stack:**
    *   **Language:** Kotlin
    *   **Framework:** Spring Boot
    *   **Build Tool:** Maven
    *   **Database:** PostgreSQL with PostGIS
    *   **Search Engine:** Elasticsearch
    *   **Messaging:** Apache Kafka
    *   **Authentication/Authorization:** Spring Security with OAuth2 (Keycloak)

## Future Vision

The platform will evolve by adding more specialized backend services, BFFs for different client experiences (e.g., web, mobile), and the corresponding frontend applications. The initial backend service provides the core location-based service capabilities that will be fundamental for many of the bookable items on the platform.