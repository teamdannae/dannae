FROM openjdk:17-jdk AS builder
WORKDIR /app

# RUN apt-get update && apt-get install -y findutils
RUN microdnf install findutils

# Gradle 파일들만 먼저 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Gradle 실행 권한 부여
RUN sed -i 's/\r$//' gradlew && \
    chmod +x ./gradlew

# 소스 복사
COPY src src

# 빌드
RUN ./gradlew bootJar

# 실행 환경
FROM openjdk:17-slim
WORKDIR /app

# builder에서 생성된 jar 파일만 복사
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]