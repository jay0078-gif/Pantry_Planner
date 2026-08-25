FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -B -DskipTests clean package

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN chown 10001:10001 /app

COPY --from=build --chown=10001:10001 /workspace/target/Pantry_Planner-0.0.1-SNAPSHOT.war app.war

USER 10001:10001

EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["java", "-jar", "/app/app.war"]
