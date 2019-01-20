FROM debian:9-slim AS builder
LABEL maintainer="Sascha Möllering <smoell@amazon.de>"

# First step: build java runtime module

RUN set -ex && \
    apt-get update && apt-get install -y wget unzip && \
    wget https://download.java.net/java/GA/jdk11/9/GPL/openjdk-11.0.2_linux-x64_bin.tar.gz -O jdk.tar.gz -nv && \
    mkdir -p /opt/jdk && \
    tar zxvf jdk.tar.gz -C /opt/jdk --strip-components=1 && \
    rm jdk.tar.gz && \
    rm /opt/jdk/lib/src.zip

RUN /opt/jdk/bin/jlink \
    --module-path /opt/jdk/jmods \
    --verbose \
    --add-modules java.base,java.logging,java.naming,java.net.http,java.se,java.security.jgss,java.security.sasl,jdk.aot,jdk.attach,jdk.compiler,jdk.crypto.cryptoki,jdk.crypto.ec,jdk.internal.ed,jdk.internal.le,jdk.internal.opt,jdk.naming.dns,jdk.net,jdk.security.auth,jdk.security.jgss,jdk.unsupported,jdk.zipfs \
    --output /opt/jdk-minimal \
    --compress 2 \
    --no-header-files

# Second step: generate Docker run image
FROM debian:9-slim
LABEL maintainer="Sascha Möllering <smoell@amazon.de>"

COPY --from=builder /opt/jdk-minimal /opt/jdk-minimal

ENV JAVA_HOME=/opt/jdk-minimal
ENV PATH="$PATH:$JAVA_HOME/bin"

RUN mkdir /opt/app && apt-get update && apt-get install curl -y

COPY target/reactive-vertx-1.3-fat.jar /opt/app

HEALTHCHECK --interval=5s --timeout=3s --retries=3 \
  CMD curl -f http://localhost:8080/health/check || exit 1

CMD ["java", "-server", "-XX:+DoEscapeAnalysis", "-XX:+UseStringDeduplication", \
        "-XX:+UseCompressedOops", "-XX:+UseG1GC", \
        "-jar", "opt/app/reactive-vertx-1.3-fat.jar"]