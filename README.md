# Metrics Publisher Service

A high-performance, scalable metrics collection, aggregation, and querying service built with Spring Boot, designed to handle time-series metric data with real-time processing capabilities.

## 🏗️ Architecture Overview

The service implements a distributed metrics pipeline with the following components:

```
┌─────────────┐    ┌──────────────┐    ┌─────────────┐    ┌──────────────┐
│   External  │    │    Fetch     │    │    Check    │    │     Store    │
│   Services  │───▶│   Service    │───▶│   Service   │───▶│    Service   │
│             │    │              │    │             │    │              │
└─────────────┘    └──────────────┘    └─────────────┘    └──────────────┘
                           │                   │                 │
                           ▼                   ▼                 ▼
                   ┌──────────────┐    ┌──────────────┐   ┌──────────────┐
                   │     Kafka    │    │    Redis     │   │  TimescaleDB │
                   │ (raw-metrics)│    │ (cardinality │   │   (storage)  │
                   │              │    │  control)    │   │              │
                   └──────────────┘    └──────────────┘   └──────────────┘
                                                                 │
                                                                 ▼
                                                          ┌──────────────┐
                                                          │    Query     │
                                                          │   Service    │
                                                          │              │
                                                          └──────────────┘
```

### Core Components

1. **Fetch Service** (`com.metrics.service.fetch`)
   - Polls external services for metrics data
   - Scheduled execution every 10 seconds
   - Publishes raw metrics to Kafka topic `metrics-raw`


2. **Check Service** (`com.metrics.service.check`)
   - Consumes raw metrics from Kafka
   - Implements cardinality control using Redis
   - Aggregates metrics using T-Digest sketches
   - Publishes processed metrics to Kafka topic `metrics-processed` 


3. **Store Service** (`com.metrics.store`)
   - Consumes processed metrics
   - Batch inserts into TimescaleDB (batch size: 500, flush interval: 5s)
   - Optimized for high-throughput writes


4. **Query Service** (`com.metrics.query`)
   - REST API for metric queries
   - Supports aggregations: SUM, AVG, RATE, P95
   - Time-bucketing with automatic step selection

## 🚀 Key Features

- **High Cardinality Protection**: Redis-based cardinality limits (1000 series per metric, 50 labels per series)
- **Real-time Aggregation**: T-Digest sketches for quantile calculations
- **Time-series Optimization**: TimescaleDB with compression and retention policies
- **Scalable Architecture**: Kafka-based event streaming for horizontal scaling

## 📋 Prerequisites

- **Java 17** or higher
- **Docker & Docker Compose**
- **Gradle** (wrapper included)

## 🛠️ Quick Start

### 1. Infrastructure Setup

Start all required services using Docker Compose:

```bash
   docker-compose up -d
```

This will start:
- **Kafka** (localhost:9092) with Zookeeper
- **Redis** (localhost:6379)  
- **TimescaleDB** (localhost:5432)
  - Database: `postgres`
  - Username: `postgres`
  - Password: `postgres`

### 2. Database Initialization

The database schema is automatically initialized via `init.sql`:
- Creates `metric_samples` hypertable
- Configures indexes for optimal query performance
- Sets up compression (7-day policy) and retention (90-day policy)
- Configures daily chunk intervals

### 3. Application Startup

Build and run the application:

```bash
# Build the application
./gradlew build

# Run the application
./gradlew bootRun
```

The service will start on **http://localhost:8081**

### 4. Verification

Check service health:
```bash
  curl http://localhost:8081/actuator/health
```

## 📊 API Reference

### Metrics Query API

**Endpoint**: `POST /query`

**Request Body**:
```json
{
  "metricName": "service-a:cpu_usage",
  "operation": "AVG",
  "labels": {
    "host": "web-01",
    "env": "prod"
  },
  "from": "2024-01-01T00:00:00Z",
  "to": "2024-01-01T01:00:00Z"
}
```

**Supported Operations**:
- `SUM`: Sum aggregation
- `AVG`: Average aggregation  
- `RATE`: Rate calculation (per second)
- `P95`: 95th percentile using T-Digest

**Response**:
```json
{
  "series": [
    {
      "ts": "2024-01-01T00:00:00Z",
      "value": 45.2
    },
    {
      "ts": "2024-01-01T00:01:00Z", 
      "value": 47.8
    }
  ]
}
```


### Mock Data Injection (Test Profile)

**Endpoint**: `POST /internal/mock/publish`

For testing with mock data:
```json
{
  "serviceId": "test-service",
  "metrics": [
    {
      "metricName": "cpu_usage",
      "metricType": "GAUGE",
      "value": 75.5,
      "timestamp": "2024-01-01T12:00:00Z",
      "labels": {
        "host": "server-01",
        "env": "test"
      }
    }
  ]
}
```

## ⚙️ Configuration

### Application Configuration (`application.yml`)

The application uses a simplified configuration structure focused on the essential components:

```yaml
server:
  port: 8081

spring:
  profiles:
    active: development
  
  data:
    redis:
      host: localhost
      port: 6379

  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: metrics-service
      auto-offset-reset: earliest
      enable-auto-commit: false
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer

  datasource:
    url: jdbc:postgresql://localhost:5432/postgres
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
    hikari:
      data-source-properties:
        defaultRowFetchSize: 1000

fetch:
  services:
    - id: service-a
      url: http://localhost:8081/metrics

logging:
  level:
    com.metrics: INFO
    org.springframework.kafka: INFO
```

### Service Configuration

Configure external services to poll by adding them to the `fetch.services` list:

```yaml
fetch:
  services:
    - id: service-b
      url: http://your-service:8080/metrics
    - id: service-c
      url: http://another-service:9090/prometheus/metrics
```


## 🔧 Development


### Code Structure
```
src/main/java/com/metrics/
├── config/             # Spring configuration
├── constants/          # Application constants
├── controllers/        # Query and Mock Data Injection API controller
├── exceptions/         # Custom exceptions
├── kafka/              # Kafka message publishing
├── models/             # Data models
├── services/           # Services
├     ├── check/              # Metric processing and aggregation
├     ├── fetch/              # External service polling
├     ├── query/              # Query processing and API
├     └── store/              # Database operations
└── utils/              # Utility classes (T-Digest, time handling)
```




## 🐛 Troubleshooting

### Common Issues

1. **Kafka Connection Issues**
   ```bash
   # Check Kafka is running
   docker ps | grep kafka
   # Verify topics exist
   docker exec kafka kafka-topics --list --bootstrap-server localhost:9092
   ```

2. **Redis Connection Problems**
   ```bash
   # Test Redis connectivity
   docker exec redis redis-cli ping
   ```

3. **TimescaleDB Schema Issues**
   ```bash
   # Connect to database
   docker exec -it timescaledb psql -U postgres -d postgres
   # Verify hypertable
   \d metric_samples
   ```

4. **High Memory Usage**
   - Check Redis memory: `docker exec redis redis-cli info memory`
   - Monitor cardinality: Check series count per metric
   - Tune JVM heap size: `-Xmx2g -Xms1g`


## 📝 License

This project is part of a metrics infrastructure demonstration.

---

