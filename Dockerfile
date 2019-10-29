FROM openjdk:7
MAINTAINER kgwack@gmail.com
COPY ./build/libs/ /usr/src/e2ewd
WORKDIR /usr/src/e2ewd
CMD ["java", "-jar", "e2ewd.jar"]