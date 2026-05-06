FROM maven:3.9.9-eclipse-temurin-24 AS build
WORKDIR /app
COPY . .

RUN mvn install:install-file -Dfile=libs/montserrat-font.jar -DgroupId=custom.fonts -DartifactId=montserrat -Dversion=1.0 -Dpackaging=jar

RUN mvn clean package -DskipTests

FROM eclipse-temurin:24-jdk
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]