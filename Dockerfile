FROM openjdk:17-slim AS build
RUN ["jlink", "--compress=2", "--strip-debug", "--no-header-files", \
     "--add-modules", "java.base,java.logging,java.sql,java.desktop,java.management,java.naming,jdk.unsupported", \
     "--output", "/var/tmp/jre"]
RUN ["apt", "update"]
RUN ["apt-get", "install", "-y", "binutils"]
RUN ["strip", "-p", "--strip-unneeded", "/var/tmp/jre/lib/server/libjvm.so"]
RUN mkdir /opt/app
WORKDIR /opt/app
COPY build.gradle settings.gradle gradlew ./
RUN chmod 755 ./gradlew
COPY gradle ./gradle
RUN ./gradlew build || return 0
COPY src ./src
RUN ./gradlew assemble -Pgpu

FROM nvidia/cuda:10.2-cudnn8-devel-ubuntu18.04
COPY --from=build /var/tmp/jre /opt/jre
ENV PATH=$PATH:/opt/jre/bin
RUN mkdir /app
COPY --from=build /opt/app/build/libs/libs /app/libs
COPY --from=build /opt/app/build/libs/app.jar /app
COPY /models /models
EXPOSE 8080
CMD ["java", "-jar", "/app/app.jar"]