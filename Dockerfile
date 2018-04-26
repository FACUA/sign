FROM ubuntu:16.04

RUN apt-get update
RUN apt-get install -y openjdk-8-jdk-headless openjfx

VOLUME /out
RUN echo "#!/bin/bash" > /usr/local/bin/export-jar && \
    echo "cp \$(ls -1r /app/build/libs/sign-prod-*.jar | head -n 1) /out/facua-sign.jar" >> /usr/local/bin/export-jar && \
    chmod +x /usr/local/bin/export-jar

WORKDIR /app

# We download the gradle distribution without including the whole /app directroy
# so each time we have to rebuild the image we can run from cache.
ADD ./gradle /app/gradle
ADD ./gradlew /app
RUN ./gradlew --no-daemon wrapper

# We do the same here, but with dependencies
ADD ./build.gradle /app
ADD ./settings.gradle /app
RUN ./gradlew --no-daemon dependencies

# We finally build the project
ADD . /app
RUN ./gradlew --no-daemon shadowJar
