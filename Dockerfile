FROM openjdk:8
MAINTAINER kgwack@gmail.com
COPY ./build/libs/ /usr/src/e2ewd
WORKDIR /usr/src/e2ewd
RUN apt update -y&&apt install nano -y
RUN chmod +x run.sh
CMD ["java", "-Dlogback.configurationFile='./logback.xml'", "-jar", "e2ewd.jar"]