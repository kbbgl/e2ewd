FROM openjdk:8
MAINTAINER kgwack@gmail.com
COPY ./build/libs/ /usr/src/e2ewd
WORKDIR /usr/src/e2ewd
RUN apt update&&apt install nano
CMD ["java", "-Dlogback.configurationFile='./logback.xml'", "-jar", "e2ewd.jar"]