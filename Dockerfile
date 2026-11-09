# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY mvnw pom.xml LICENSE NOTICE ./
COPY .mvn/ .mvn/
COPY shadwick-core/ shadwick-core/
COPY shadwick-examples/ shadwick-examples/
# Railway requires literal cache IDs scoped to this service (not build-arg interpolation).
RUN --mount=type=cache,id=s/794b0087-a4ca-44ff-8ecd-375e87f53a24-/root/.m2,target=/root/.m2 \
    --mount=type=cache,id=s/794b0087-a4ca-44ff-8ecd-375e87f53a24-/root/.npm,target=/root/.npm \
    ./mvnw -B -pl shadwick-examples -am -DskipTests package \
    && cp shadwick-examples/target/shadwick-examples-*.jar /tmp/app.jar

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app
RUN groupadd --system shadwick \
    && useradd --system --gid shadwick --home-dir /app shadwick \
    && chown shadwick:shadwick /app
COPY --from=build --chown=shadwick:shadwick /tmp/app.jar /app/app.jar
USER shadwick
ENV SPRING_PROFILES_ACTIVE=railway \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65.0 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["java", "-Dwicket.configuration=deployment", "-jar", "/app/app.jar"]
