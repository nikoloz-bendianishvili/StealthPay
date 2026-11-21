# Stage 1: build the jar with the JDK and the Maven wrapper.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy only the build files first, so the dependency download is cached
# and re-runs only when pom.xml changes.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -q dependency:go-offline

COPY src src
RUN ./mvnw -q package -DskipTests

# Stage 2: run the jar with only a JRE. No JDK, Maven or source code ends up here.
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --no-create-home stealthpay
USER stealthpay

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
