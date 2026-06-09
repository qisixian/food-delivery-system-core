## Food Delivery System Core


./mvnw clean package

podman build -t food-delivery-system-core:1.0 .

podman stop food-delivery-system-core

podman rm food-delivery-system-core

podman run -d \
    --name food-delivery-system-core \
    -p 8080:8080 \
    --env-file .env \
    food-delivery-system-core:1.0

podman logs -f food-delivery-system-core
