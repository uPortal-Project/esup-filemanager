FROM maven:3.9.9-eclipse-temurin-21@sha256:3a4ab3276a087bf276f79cae96b1af04f53731bec53fb2e651aca79e4b10211e AS build
WORKDIR /build
COPY pom.xml .
COPY src/main ./src/main
COPY src/etc/header.txt ./src/etc/header.txt
COPY src/etc/docker/e2e ./src/etc/docker/e2e
RUN cp src/etc/docker/e2e/drives-e2e.xml src/main/resources/drives.xml
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package

FROM eclipse-temurin:21.0.8_9-jre-noble@sha256:20e7f7288e1c18eebe8f06a442c9f7183342d9b022d3b9a9677cae2b558ddddd
WORKDIR /app
COPY --from=build /build/target/esup-filemanager.war /app/app.war
COPY src/etc/docker/e2e/application-e2e.properties /app/application-e2e.properties
USER 1000:1000
ENTRYPOINT ["java", "-Dspring.devtools.restart.enabled=false", "-jar", "/app/app.war", "--spring.config.additional-location=file:/app/application-e2e.properties"]
