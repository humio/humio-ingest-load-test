FROM  azul/zulu-openjdk-alpine:17
ENV SCALA_VERSION=2.13.12 \
  SCALA_HOME=/usr/share/scala \
  SBT_VERSION=1.9.6

COPY . /humio-ingest-load-test-build/
RUN \
  apk update && \
  apk upgrade && \
  apk add --no-cache --virtual=.build-dependencies wget ca-certificates && \
  apk add --no-cache bash && \
  apk add --no-cache make && \
  cd "/tmp" && \
  wget "https://downloads.typesafe.com/scala/${SCALA_VERSION}/scala-${SCALA_VERSION}.tgz" && \
  tar xzf "scala-${SCALA_VERSION}.tgz" && \
  mkdir "${SCALA_HOME}" && \
  rm "/tmp/scala-${SCALA_VERSION}/bin/"*.bat && \
  mv "/tmp/scala-${SCALA_VERSION}/bin" "/tmp/scala-${SCALA_VERSION}/lib" "${SCALA_HOME}" && \
  ln -s "${SCALA_HOME}/bin/"* "/usr/bin/" && \
  apk del .build-dependencies && \
  rm -rf "/tmp/"* 

RUN \  
  echo "$SCALA_VERSION $SBT_VERSION" && \
  apk add --no-cache bash curl bc ca-certificates && \
  update-ca-certificates && \
  scala -version && \
  scalac -version && \
  curl -fsL https://github.com/sbt/sbt/releases/download/v$SBT_VERSION/sbt-$SBT_VERSION.tgz | tar xfz - -C /usr/local && \
  $(mv /usr/local/sbt-launcher-packaging-$SBT_VERSION /usr/local/sbt || true) && \
  ln -s /usr/local/sbt/bin/* /usr/local/bin/ && \
  apk del curl && \
  sbt -Dsbt.rootdir=true sbtVersion && \
  cd /humio-ingest-load-test-build && \
  make && \
  mkdir /humio-ingest-load-test && \
  cp /humio-ingest-load-test-build/target/scala-2.13/perftest.jar /humio-ingest-load-test/ && \
  cp /humio-ingest-load-test-build/entrypoint.sh /humio-ingest-load-test/ && \
  cp -R /humio-ingest-load-test-build/templates /humio-ingest-load-test/templates && \
  rm -rf /humio-ingest-load-test-build && \
  rm -rf /home/sbtuser && \
  rm -rf /root/* /root/.sbt /root/.ivy2 /root/.bashrc /root/.profile /root/.sbt /root/.cache /root/.wget-hsts && \
  chmod +x /humio-ingest-load-test/entrypoint.sh

WORKDIR /humio-ingest-load-test
ENTRYPOINT ["/humio-ingest-load-test/entrypoint.sh"]
