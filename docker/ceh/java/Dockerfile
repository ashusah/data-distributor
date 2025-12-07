FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY SimpleSignalServer.java .
RUN javac SimpleSignalServer.java
EXPOSE 5000
CMD ["java", "SimpleSignalServer"]