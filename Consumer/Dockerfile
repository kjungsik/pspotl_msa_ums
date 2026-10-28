# 기존: FROM eclipse-temurin:17-jre-alpine
# 변경: Apple Silicon(M1/M2/M3) 및 Intel Mac 모두에서 안정적인 Corretto 17 Alpine 사용
FROM amazoncorretto:17-alpine-jdk

WORKDIR /app

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

ENV TZ=Asia/Seoul
ENV JAVA_OPTS="-Xms128m -Xmx256m"

EXPOSE 8081

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]