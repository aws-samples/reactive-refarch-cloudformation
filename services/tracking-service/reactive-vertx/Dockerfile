FROM debian:10-slim AS builder
LABEL maintainer="Sascha Möllering <smoell@amazon.de>"

# First step: build java runtime module

RUN apt-get update 
RUN apt-get install wget -y 
RUN mkdir -p /usr/share/man/man1
RUN apt-get install openjdk-11-jdk-headless -y

RUN wget https://mirror.softaculous.com/apache/maven/maven-3/3.6.3/binaries/apache-maven-3.6.3-bin.tar.gz -P /tmp
RUN tar xf /tmp/apache-maven-3.6.3-bin.tar.gz -C /opt
RUN ln -s /opt/apache-maven-3.6.3 /opt/maven

ENV M2_HOME=/opt/maven
ENV MAVEN_HOME=/opt/maven
ENV PATH=${M2_HOME}/bin:${PATH}

COPY ./pom.xml ./pom.xml
COPY src ./src/

ENV MAVEN_OPTS='-Xmx6g'

RUN mvn -Dmaven.test.skip=true clean package

RUN set -ex && \
    apt-get update && apt-get install -y wget unzip && \
    wget https://corretto.aws/downloads/latest/amazon-corretto-15-x64-linux-jdk.tar.gz -nv && \
    mkdir -p /opt/jdk && \
    tar zxvf amazon-corretto-15-x64-linux-jdk.tar.gz -C /opt/jdk --strip-components=1 && \
    rm amazon-corretto-15-x64-linux-jdk.tar.gz && \
    rm /opt/jdk/lib/src.zip

RUN /opt/jdk/bin/jlink \
    --module-path /opt/jdk/jmods \
    --verbose \
    --add-modules java.base,java.logging,java.naming,java.net.http,java.se,java.security.jgss,java.security.sasl,jdk.aot,jdk.attach,jdk.compiler,jdk.crypto.cryptoki,jdk.crypto.ec,jdk.internal.ed,jdk.internal.le,jdk.internal.opt,jdk.naming.dns,jdk.net,jdk.security.auth,jdk.security.jgss,jdk.unsupported,jdk.zipfs \
    --output /opt/jdk-minimal \
    --compress 2 \
    --no-header-files

# Second step: generate Docker run image
FROM debian:10-slim
LABEL maintainer="Sascha Möllering <smoell@amazon.de>"

RUN mkdir /opt/app && apt-get update && apt-get install curl -y

COPY --from=builder /opt/jdk-minimal /opt/jdk-minimal
COPY --from=builder target/reactive-vertx-2.0-fat.jar /opt/app/reactive-vertx-2.0-fat.jar

ENV JAVA_HOME=/opt/jdk-minimal
ENV PATH="$PATH:$JAVA_HOME/bin"

HEALTHCHECK --interval=5s --timeout=3s --retries=3 \
  CMD curl -f http://localhost:8080/health/check || exit 1

EXPOSE 8080

CMD ["java", "-server", "-XX:+DoEscapeAnalysis", "-XX:+UseStringDeduplication", \
        "-XX:+UseCompressedOops", "-XX:+UseG1GC", \
        "-jar", "opt/app/reactive-vertx-1.6-fat.jar"]
