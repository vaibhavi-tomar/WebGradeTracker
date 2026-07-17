# Use a small official Java runtime image that includes the compiler (JDK)
FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

# Copy both project folders into the image
COPY backend ./backend
COPY frontend ./frontend

# Compile all the Java source files
RUN javac backend/*.java -d backend

# The hosting platform tells us which port to listen on via $PORT
EXPOSE 8080

# Run the server (Server.java reads the PORT env variable itself)
CMD ["java", "-cp", "backend", "Server"]
