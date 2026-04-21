FROM eclipse-temurin:21-jdk-alpine AS builder
RUN apk add --no-cache unzip
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

COPY libs/notification-events/pom.xml libs/notification-events/
RUN mkdir -p libs/notification-events/src/main/java

# Parent pom.xml lists all service modules — paths must exist even when building only notification.
COPY services/account/pom.xml services/account/
RUN mkdir -p services/account/src/main/java
COPY services/payment/pom.xml services/payment/
RUN mkdir -p services/payment/src/main/java
COPY services/cashback/pom.xml services/cashback/
RUN mkdir -p services/cashback/src/main/java

COPY services/notification/pom.xml services/notification/
RUN mkdir -p services/notification/src/main/java

RUN ./mvnw -B -N install

COPY libs/notification-events/src libs/notification-events/src
COPY services/notification/src services/notification/src

RUN ./mvnw -B -pl services/notification -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/services/notification/target/notification-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8093
ENTRYPOINT ["java", "-jar", "app.jar"]
