FROM amazoncorretto:21-alpine

RUN apk add --no-cache curl

COPY target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]