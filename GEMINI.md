# Project Context: BookAnything Platform

This document provides a comprehensive overview of the `BookAnything` platform, detailing its overarching architecture, features, and current state as a multi-application ecosystem.

## Overview

The `BookAnything` platform is a monorepo designed to host a variety of backend and frontend applications. Its primary goal is to provide a flexible and scalable foundation for diverse booking-related services, ranging from localized booking systems to broader event management solutions.

## Key Features and Functionality (Platform Level)

*   **Modular Architecture:** The platform is designed with modularity in mind, allowing for independent development, deployment, and scaling of individual backend services, frontend applications, and shared components.
*   **Diverse Application Support:** Accommodates various types of applications, including:
    *   **Backend Services:** RESTful APIs, microservices, and data processing units (e.g., `distribution-center-locator`).
    *   **Frontend Applications:** Web applications, mobile applications, and potentially desktop clients.
    *   **Backend-for-Frontend (BFF) Services:** Tailored APIs for specific frontend needs.
*   **Shared Utilities:** Centralized location for common libraries, tools, and configurations to promote code reuse and consistency across the platform.

## Current State

The `BookAnything` platform is under active development, with initial focus on establishing core backend services and foundational infrastructure. The monorepo structure facilitates integrated development and ensures consistency across different components.

Individual sub-projects within the monorepo will have their own `README.md` and `GEMINI.md` files detailing their specific architecture, features, and technology stacks.
