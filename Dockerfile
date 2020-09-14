FROM maven:3.6.0-jdk-8

LABEL maintainer="martynas@atomgraph.com"

COPY . /usr/src/CSV2RDF

WORKDIR /usr/src/CSV2RDF

RUN mvn clean install

### entrypoint

ENTRYPOINT ["java", "-jar", "target/csv2rdf-jar-with-dependencies.jar"]