# Food Delivery System Core

A food delivery backend. Provides APIs for customers and administrators.

- Customer frontend: https://github.com/qisixian/food-delivery-system-web-app
- Admin frontend: https://github.com/qisixian/food-delivery-system-web-admin


## Quick Start

Requires Podman and a running MySQL instance. Redis is optional (falls back to database queries when unavailable). 

Run all commands from the project root.

#### 1. Initialize the database

Use `db-init-mysql.sql` to initialize the database.

#### 2. Configure environment variables

```bash
cp .env.example .env
```
Edit `.env` with your settings. For services running on the host machine, use `host.containers.internal` instead of `localhost`.

#### 3. Start the services

```bash
podman compose up -d
```

This starts the published backend image and monitoring services. MySQL and Redis are managed separately.

Stop and remove the containers:

```bash
podman compose down 
```

## Development

```bash
# Run tests and generate coverage reports
./mvnw clean verify

# Also run SonarQube analysis (requires a running SonarQube server)
./mvnw clean verify sonar:sonar -Dsonar.token="$SONAR_TOKEN"

# Stop existing services
podman compose stop

# Start existing services
podman compose start

# Update the backend
podman compose pull food-delivery-system-core
podman compose up -d --no-deps food-delivery-system-core

# View backend logs
podman compose logs -f food-delivery-system-core
```


## Set up Grafana

http://localhost:3000/login
- username: admin
- password: admin

#### Configure data sources
- connections -> data source -> add new data source -> prometheus -> Prometheus server URL: http://host.containers.internal:9090
- connections -> data source -> add new data source -> loki -> URL: http://host.containers.internal:3100
- connections -> data source -> add new data source -> tempo -> URL: http://host.containers.internal:3200

#### Check connections
- explore -> prometheus -> Switch to Code mode -> Query: up{job="springboot"} -> Run query
- explore -> loki -> Switch to Code mode -> Query: {service_name="food-delivery-system-core"} -> Run query
- explore -> tempo -> Query type: search -> Run query

#### Import dashboard
- dashboards -> new -> import -> upload dashboard json file -> `ops/grafana-prometheus-dashboard.json`



## SonarQube
Only a local SonarQube server for now.

#### SonarQube Disabled Rules
- java:S6813 - Field dependency injection should be avoided
