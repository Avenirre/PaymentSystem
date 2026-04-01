FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw

COPY services/payment/pom.xml services/payment/
RUN mkdir -p services/payment/src/main/java

RUN ./mvnw -B -N install

COPY services/account/pom.xml services/account/
COPY services/account/src services/account/src

RUN ./mvnw -B -f services/account/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/services/account/target/account-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
