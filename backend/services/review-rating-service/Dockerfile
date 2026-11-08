# ============================================================================
# Dockerfile for Review & Rating Service (Port 8085)
# LearnHub LMS - MSS301 Microservices Project
# ============================================================================

# Stage 1: Runtime base image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

VOLUME /tmp

# Argument for jar file
ARG JAR_FILE=target/review-rating-service-*.jar
COPY ${JAR_FILE} app.jar

ENV PORT=8085
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8085

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
