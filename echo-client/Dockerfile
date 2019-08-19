FROM openjdk:10-jdk

WORKDIR /app
COPY . .
RUN ./mvnw -DskipTests package

CMD ["java", "-jar", "target/echo-client-0.0.1-SNAPSHOT.jar"]
