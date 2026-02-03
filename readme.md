# Scalable Systems Integration Pipeline- 2nd fashion using apache camel

## Reasons- one is i have not worked with python integration might have been ununiform, others;
- Unified JVM stack – Everything runs in Spring Boot, no cross-language complexity. 
- Native Kafka support – Camel has built-in Kafka components, making routing, transformation, and retries easy. 
- Idempotency & error handling – Camel has built-in patterns for deduplication, retries, dead-letter queues. 
- Scalability – Easier to scale Java apps and manage multiple endpoints without Python async overhead. 
- Maintainability – One codebase, easier monitoring, logging, and CI/CD.

## Overview
This project demonstrates a scalable integration pipeline using **Java Spring Boot producers** and **Python consumers** with **Kafka** as the message broker and **Redis** for idempotency.  
It fetches data from CRM and Inventory systems, merges it, and sends it to an Analytics system.  

# Scalable Systems Integration Pipeline

## Overview
This project demonstrates a scalable integration pipeline using **Java Spring Boot producers** and **Python consumers** with **Kafka** as the message broker and **Redis** for idempotency.  
It fetches data from CRM and Inventory systems, merges it, and sends it to an Analytics system.  


---

## Architecture & Design Decisions

- **Producers (Java Spring Boot)**
    - Fetch from CRM (`/customers`) and Inventory (`/products`) using `WebClient`.
    - Publish messages to Kafka topics: `customer_data` and `inventory_data`.
    - Retry logic with exponential backoff for failed API calls.
    - Modular design allows adding new data sources easily.

- **Consumers (Apache Camel routes in Java/Spring Boot)**
    - Consume messages from Kafka topics (customer_data and inventory_data) natively using Camel Kafka component.
    - Merge messages by id within Camel routes to create a combined JSON payload.
    - Deduplicate using Redis (SHA256 hash keys, TTL 24 hours) via Camel’s Redis component.
    - Retry failed posts to Analytics with Camel’s retry/error handling patterns and log errors centrally.

- **Message Broker (Kafka)**
    - Provides decoupling between producers and consumers.
    - Supports scalability via partitions.

- **Idempotency (Redis)**
    - Prevents duplicate processing of messages.

- **Scalability & Reliability**
    - Event-driven architecture allows multiple consumer instances.
    - Retry and backoff strategies for API/network failures.
    - Designed to handle 10,000+ records/hour.

---

## Prerequisites

- Docker & Docker Compose
- Java 17+ (for Spring Boot producers)
- Python 3.10+ (for consumers)

---

## Setup & Run

1. **Clone repository**
```bash
git clone git@github.com:Emmanuel-Mwiti/Integration-test.git
cd Integration-test
docker-compose up --build
docker ps
docker logs producer-app -f       # Check producers sending messages
docker logs consumer-app -f       # Check consumer merging and sending to analytics
docker logs analytics-mock -f    # Verify analytics received merged payloads
```
## Environment Variables (Docker Compose)

| Service       | Variable                     | Default     |
|---------------|-----------------------------|------------|
| Producer-App  | SPRING_KAFKA_BOOTSTRAP_SERVERS | kafka:9092 |
| Consumer-App  | KAFKA_BOOTSTRAP_SERVERS        | kafka:9092 |
| Consumer-App  | REDIS_HOST                     | redis      |

## Test Result

![Test Result](test_with_apache%20camel.png)
To test, You can directly:
```bash
docker logs analytics-mock -f  
```
Then see logs like:
```
Analytics received: {customer={id=1, name=Emmanuel Mwiti, email=emmanuel@example.com, updatedAt=null}, inventory={id=1, name=Laptop, stock=15, updatedAt=null}}  
Analytics received: {customer={id=2, name=Victor Mugo, email=victor@example.com, updatedAt=null}, inventory={id=2, name=Mouse, stock=50, updatedAt=null}}        
Analytics received: {customer={id=1, name=Emmanuel Mwiti, email=emmanuel@example.com, updatedAt=null}, inventory={id=1, name=Laptop, stock=15, updatedAt=null}}  
Analytics received: {customer={id=2, name=Victor Mugo, email=victor@example.com, updatedAt=null}, inventory={id=2, name=Mouse, stock=50, updatedAt=null}}

```
This is a log in the mock analytics, that is consuming the fianl refined data

*This image shows the test result.*

