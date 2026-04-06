FROM eclipse-temurin:21-jdk-alpine AS builder
RUN apk add --no-cache unzip
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

COPY services/account/pom.xml services/account/
RUN mkdir -p services/account/src/main/java

COPY services/payment/pom.xml services/payment/
RUN mkdir -p services/payment/src/main/java

RUN ./mvnw -B -N install

COPY services/cashback/pom.xml services/cashback/
COPY services/cashback/src services/cashback/src

RUN ./mvnw -B -f services/cashback/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/services/cashback/target/cashback-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8092
ENTRYPOINT ["java", "-jar", "app.jar"]
