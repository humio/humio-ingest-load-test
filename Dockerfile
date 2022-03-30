FROM azul/zulu-openjdk-debian:11 
ARG SBT_VERSION=1.5.7

COPY . /humio-ingest-load-test-build/
# Install sbt
RUN \
  apt-get update && \
  apt-get install make curl -y && \
  apt-get dist-upgrade -y && \
  mkdir /working/ && \
  cd /working/ && \
  curl -L -o sbt-$SBT_VERSION.deb https://repo.scala-sbt.org/scalasbt/debian/sbt-$SBT_VERSION.deb && \
  dpkg -i sbt-$SBT_VERSION.deb && \
  rm sbt-$SBT_VERSION.deb && \
  cd && \
  rm -r /working/ && \
  rm -rf /var/lib/apt/lists/* && \
  cd /humio-ingest-load-test-build && \
  make && \
  mkdir /humio-ingest-load-test && \
  cp /humio-ingest-load-test-build/target/scala-2.12/perftest.jar /humio-ingest-load-test/ && \
  cp /humio-ingest-load-test-build/entrypoint.sh /humio-ingest-load-test/ && \
  cp -R /humio-ingest-load-test-build/templates /humio-ingest-load-test/templates && \
  rm -rf /humio-ingest-load-test-build && \
  rm -rf /home/sbtuser && \
  rm -rf /root/* /root/.sbt /root/.ivy2 /root/.bashrc /root/.profile /root/.sbt /root/.cache /root/.wget-hsts && \
  chmod +x /humio-ingest-load-test/entrypoint.sh

WORKDIR /humio-ingest-load-test
ENTRYPOINT ["/humio-ingest-load-test/entrypoint.sh"]
