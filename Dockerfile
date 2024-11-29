# Step 1: Use an official OpenJDK 21 image as the base image
FROM eclipse-temurin:21-jdk-alpine

# Step 2: Set the working directory inside the container
WORKDIR /app

# Step 3: Copy the Spring Boot JAR file into the container
COPY target/ReceiptProcessor-0.0.1-SNAPSHOT.jar app.jar

# Step 4: Expose the port your application runs on (default is 8080 for Spring Boot)
EXPOSE 8080

# Step 5: Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
