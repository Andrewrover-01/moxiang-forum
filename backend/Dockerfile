# ──────────────────────────────────────────────
# Stage 1 – Build with Maven
# ──────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY settings.xml .
# Copy POMs first so dependency resolution is cached as a separate layer
COPY pom.xml .
COPY moxiang-common/pom.xml  moxiang-common/
COPY moxiang-mbg/pom.xml     moxiang-mbg/
COPY moxiang-service/pom.xml moxiang-service/
COPY moxiang-web/pom.xml     moxiang-web/

RUN mvn dependency:go-offline -B -s settings.xml

# Copy source code
COPY moxiang-common/src  moxiang-common/src
COPY moxiang-mbg/src     moxiang-mbg/src
COPY moxiang-service/src moxiang-service/src
COPY moxiang-web/src     moxiang-web/src

RUN mvn package -DskipTests -B -s settings.xml

# ──────────────────────────────────────────────
# Stage 2 – Runtime image
# ──────────────────────────────────────────────
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /app/moxiang-web/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
