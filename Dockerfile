FROM eclipse-temurin:25-jre-alpine
RUN apk update && apk add --no-cache curl
COPY target/app.jar /app.jar
EXPOSE 7070
CMD ["java", "-jar", "/app.jar"]