FROM maven:3.6.3-jdk-11-slim

RUN apt-get update && apt-get install -y git libxrender1 libxext6 libxtst6 libfreetype6 libfontconfig1

WORKDIR /usr/src

COPY . .

CMD mvn install && java -jar ac-ui/target/*SNAPSHOT*.jar