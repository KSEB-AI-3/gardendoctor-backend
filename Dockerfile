FROM gradle:8.2-jdk17 AS build

WORKDIR /app

COPY --chown=gradle:gradle build.gradle settings.gradle ./
COPY --chown=gradle:gradle src ./src

RUN gradle build -x test --no-daemon

FROM openjdk:17-jdk-alpine

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
