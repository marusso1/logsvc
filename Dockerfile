FROM adoptopenjdk:11-jdk
ARG VERSION
ADD build/libs/logsvc-$VERSION.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
