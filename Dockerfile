FROM gradle:8.2-jdk17 AS build

WORKDIR /app

COPY --chown=gradle:gradle build.gradle settings.gradle ./
COPY --chown=gradle:gradle src ./src

RUN gradle build -x test --no-daemon

FROM openjdk:17-jdk-alpine

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

# Redis 호스트 기본값 (docker-compose에서 덮어쓰기 가능)
ENV SPRING_REDIS_HOST=redis

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
