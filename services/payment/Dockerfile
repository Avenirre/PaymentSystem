FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw

COPY services/account/pom.xml services/account/
RUN mkdir -p services/account/src/main/java

RUN ./mvnw -B -N install

COPY services/payment/pom.xml services/payment/
COPY services/payment/src services/payment/src

RUN ./mvnw -B -f services/payment/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/services/payment/target/payment-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8091
ENTRYPOINT ["java", "-jar", "app.jar"]
