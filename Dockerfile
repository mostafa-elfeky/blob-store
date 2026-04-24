FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src

RUN chmod +x gradlew
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

ENV BLOBSTORE_CONFIG_DIR=/var/lib/blob-store/config
ENV BLOBSTORE_STORAGE_ROOT_DIR=/var/lib/blob-store/storage

COPY --from=build /workspace/build/libs/*.jar /app/blob-store.jar
COPY docker/blob-store-entrypoint.sh /app/blob-store-entrypoint.sh

RUN chmod +x /app/blob-store-entrypoint.sh \
    && mkdir -p /var/lib/blob-store/config /var/lib/blob-store/storage

EXPOSE 4817
VOLUME ["/var/lib/blob-store"]

ENTRYPOINT ["/app/blob-store-entrypoint.sh"]
