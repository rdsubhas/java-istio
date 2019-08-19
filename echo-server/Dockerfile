FROM openjdk:11-jdk

WORKDIR /app
COPY . .
RUN ./mvnw -DskipTests package

CMD ["java", "-jar", "target/echo-server-0.0.1-SNAPSHOT.jar"]
