FROM alpine:3.8

ARG OPENJDK11_ALPINE_URL=https://download.java.net/java/early_access/alpine/27/binaries/openjdk-11+27_linux-x64-musl_bin.tar.gz
RUN apk add --no-cache \
  ca-certificates \
  wget \
  curl \
  bash \
  procps

RUN mkdir -p /opt/jdk \
  && wget -c -O- $OPENJDK11_ALPINE_URL \
  | tar -zxC /opt/jdk \
  && rm /opt/jdk/jdk-11/lib/src.zip

ENV JAVA_HOME /opt/jdk/jdk-11
ENV PATH=$PATH:$JAVA_HOME/bin
ENV LANG=C.UTF-8

WORKDIR /srv/vertx

HEALTHCHECK --interval=10s --timeout=3s \
  CMD curl -f -s http://localhost:8080/health/check || exit 1

ADD target/reactive-vertx-1.2-fat.jar srv/vertx/

CMD ["java", "-server", "-XX:+DoEscapeAnalysis", "-XX:+UseStringDeduplication", \
        "-XX:+UseCompressedOops", "-Xms200M", "-Xmx200M", "-XX:+UseG1GC", \
        "-jar", "srv/vertx/reactive-vertx-1.2-fat.jar"]