## Food Delivery System Core

### compose
```bash
podman compose up -d
podman compose down 
```

### command
```bash

#### app

./mvnw clean verify sonar:sonar -Dsonar.token="$SONAR_TOKEN"

./mvnw clean package
podman build -t food-delivery-system-core:1.0 .
podman stop food-delivery-system-core
podman rm food-delivery-system-core

podman run -d \
    --name food-delivery-system-core \
    -p 8080:8080 \
    --env-file .env \
    food-delivery-system-core:latest

podman logs -f food-delivery-system-core
podman rmi food-delivery-system-core
podman pull ghcr.io/qisixian/food-delivery-system-core:latest


用 compose pull 新镜像
podman compose pull food-delivery-system-core

找到当前容器的老镜像ID
podman container inspect food-delivery-system-core --format '{{.Image}}'

更新镜像
podman compose up -d --no-deps --force-recreate food-delivery-system-core

按ID删除老镜像：
podman rmi 30172b9f93a81eca336d0e15a3bf2f6b312f0b2bc61a73e77f5a370bccf4bb43

如果需要停止镜像
podman compose stop food-delivery-system-core


##### otel-collector

podman pull otel/opentelemetry-collector:0.154.0

podman run -d \
    --name otel-collector \
    -p 4317:4317 \
    -p 4318:4318 \
    -v ./ops/otel-collector-config.yaml:/etc/otelcol/config.yaml \
    otel/opentelemetry-collector:latest

podman start otel-collector
podman stop otel-collector
podman rm otel-collector


##### prometheus

podman pull prom/prometheus

podman run -d \
    --name prometheus \
    -p 9090:9090 \
    -v ./ops/prometheus.yml:/etc/prometheus/prometheus.yml \
    prom/prometheus:latest

podman logs -f prometheus
podman start prometheus
podman stop prometheus
podman rm prometheus

http://localhost:9090/query

##### loki

podman pull grafana/loki:latest

podman run -d \
    --name loki \
    -p 3100:3100 \
    -v ./ops/loki-config.yaml:/etc/loki/loki-config.yaml \
    grafana/loki:latest

// -config.file=/etc/loki/loki-config.yaml // 好像会默认读取这个文件

podman start loki
podman stop loki
podman rm loki


#### tempo

podman pull grafana/tempo:latest

podman run -d \
  --name tempo \
  -p 3200:3200 \
  -p 4319:4319 \
  -p 4320:4320 \
  -v ./ops/tempo.yaml:/etc/tempo.yaml \
  grafana/tempo:latest \
  -config.file=/etc/tempo.yaml

#   --network observability \ # 这样可以不走宿主机，直接容器间通信，更推荐

podman stop tempo
podman rm tempo


##### grafana

podman pull grafana/grafana

podman run -d \
    --name grafana \
    -p 3000:3000 \
    grafana/grafana:latest
    
podman stop grafana
podman rm grafana

http://localhost:3000/login
connections -> data source -> add new data source -> prometheus -> Prometheus server URL: http://host.containers.internal:9090
connections -> data source -> add new data source -> loki -> URL: http://host.containers.internal:3100
connections -> data source -> add new data source -> tempo -> URL: http://host.containers.internal:3200
explore -> prometheus -> [Builder❌->Code✅] -> Query: up{job="food-delivery-system-core"} -> Run query
explore -> loki -> Label filters: service_name = food-delivery-system-core -> Run query
explore -> tempo -> Query type: search -> Run query
    

#### into the container
podman machine ssh
podman exec -it otel-collector sh


#### SonarQube

podman run -d \
  --name sonarqube \
  -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true \
  -p 9002:9000 \
  sonarqube:community

podman stop sonarqube
podman rm sonarqube

```

### SonarQube
No CI sonar yet, because Github Action cannot reach local SonarQube server.

#### SonarQube Disabled Rules
java:S6813 - Field dependency injection should be avoided
