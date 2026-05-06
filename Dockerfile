FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM amazoncorretto:17-alpine

RUN apk add --no-cache curl

COPY --from=build /build/target/app.jar /app.jar

EXPOSE 7030

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 CMD curl -fsS "http://localhost:${PORT:-7030}/api/health" || exit 1

CMD ["sh", "-c", "java ${JAVA_OPTS:-} -XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError -jar /app.jar"]
