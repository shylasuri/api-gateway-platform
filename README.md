# API Gateway Platform

A full-stack API Gateway for managing API access, authentication, rate limiting, usage quotas, analytics, and usage-based billing.

Built with **Spring Boot, React, PostgreSQL, and Redis**.

## Features

* JWT-based authentication
* Consumer and admin access
* API registration and management
* Configurable rate limiting:

  * Fixed Window
  * Sliding Window
  * Token Bucket
  * Leaky Bucket
* Per-API rate-limit configuration
* Usage and quota tracking
* Usage analytics
* Subscription plans
* Usage-based billing
* Stripe test-mode integration
* Swagger/OpenAPI documentation
* Docker-based deployment

## Architecture

```text
                         ┌──────────────────┐
                         │   React Frontend │
                         └────────┬─────────┘
                                  │
                                  ▼
                          ┌─────────────────┐
                          │  Spring Boot    │
                          │  API Gateway    │
                          │                 │
                          │ Authentication  │
                          │ Rate Limiting   │
                          │ Quotas          │
                          │ Usage / Billing │
                          └───────┬─────┬───┘
                                  │     │
                         ┌────────▼─┐ ┌─▼────────┐
                         │PostgreSQL│ │  Redis   │
                         └──────────┘ └──────────┘
                                  │
                          ┌───────▼───────┐
                          │ Backend APIs  │
                          └───────────────┘
```

### Request Flow

```text
Client
  │
  ▼
API Gateway
  │
  ├── Authenticate Request
  ├── Rate Limit Check
  ├── Quota Check
  ├── Forward Request
  └── Record Usage
          │
          ▼
     Backend API
```

## Tech Stack

### Backend

* Java 21
* Spring Boot
* Spring Security
* Spring Data JPA
* PostgreSQL
* Redis
* JWT
* Stripe
* Maven

### Frontend

* React
* TypeScript
* Vite
* Tailwind CSS
* Axios
* React Router
* Recharts

### Infrastructure

* Docker
* Docker Compose

## Project Structure

```text
api-gateway-platform/
├── backend/          # Spring Boot backend
├── frontend/         # React dashboard
├── sample-api/       # Sample backend API
├── benchmark/        # Rate-limit benchmarking
├── docker-compose.yml
├── .env.example
└── README.md
```

## Getting Started

### Prerequisites

* Docker Desktop
* Git

### Run Locally

```bash
git clone <repository-url>
cd api-gateway-platform
cp .env.example .env
```

Configure the required environment variables in `.env`, then run:

```bash
docker compose up --build
```

### Services

| Service    | Port |
| ---------- | ---: |
| Frontend   | 5173 |
| Backend    | 8080 |
| Sample API | 8081 |
| PostgreSQL | 5432 |
| Redis      | 6379 |

Stop the application with:

```bash
docker compose down
```

## Environment Configuration

The application uses environment variables for database, Redis, authentication, frontend, and Stripe configuration.

Example variables:

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
REDIS_HOST
REDIS_PORT
JWT_SECRET
FRONTEND_URL
SAMPLE_API_URL
STRIPE_SECRET_KEY
STRIPE_WEBHOOK_SECRET
```

Sensitive credentials should be provided through environment variables or a secret-management system in deployed environments.

## API Documentation

Once the backend is running, Swagger UI is available at:

```text
http://localhost:8080/swagger-ui.html
```

The API documentation covers authentication, API management, plans, gateway operations, usage, analytics, and billing.

## Rate Limiting

The gateway supports multiple rate-limiting algorithms:

* Fixed Window
* Sliding Window
* Token Bucket
* Leaky Bucket

Redis is used for high-frequency rate-limit state, while PostgreSQL stores persistent application and usage data.

## Billing

The platform tracks API usage and supports usage-based billing.

Stripe integration is available in test mode for customer and subscription management. Stripe credentials are configured through environment variables.

## Deployment

The application is containerized using Docker and can be deployed as separate frontend, backend, database, and Redis services.

For production deployments:

* Use managed PostgreSQL and Redis where appropriate
* Store secrets using the deployment platform's secret management
* Configure HTTPS and a production domain
* Configure CORS for the deployed frontend
* Use proper database migration tooling
* Avoid publicly exposing internal database and Redis services

## Testing

Backend tests:

```bash
cd backend
mvn test
```

Frontend production build:

```bash
cd frontend
npm install
npm run build
```

## License

Private project.
