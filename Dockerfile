FROM openjdk:21-jdk

ARG DEBIAN_FRONTEND=noninteractive
ENV TZ=Asia/Seoul
RUN apk add --no-cache tzdata

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]