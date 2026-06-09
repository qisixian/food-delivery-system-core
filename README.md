## Food Delivery System Core


./mvnw clean package

podman build -t my-springboot-app:1.0 .

podman stop my-springboot-app
podman rm my-springboot-app

podman run -d \
    --name my-springboot-app \
    -p 8080:8080 \
    --env-file .env \
    my-springboot-app:1.0

podman logs -f my-springboot-app
