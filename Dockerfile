FROM azul/zulu-openjdk-debian:11 
ARG SBT_VERSION=1.5.7

# Install sbt
RUN \
  apt-get update && \
  apt-get install make curl -y && \
  mkdir /working/ && \
  cd /working/ && \
  curl -L -o sbt-$SBT_VERSION.deb https://repo.scala-sbt.org/scalasbt/debian/sbt-$SBT_VERSION.deb && \
  dpkg -i sbt-$SBT_VERSION.deb && \
  rm sbt-$SBT_VERSION.deb && \
  cd && \
  rm -r /working/ && \
  rm -rf /var/lib/apt/lists/* && \
  sbt sbtVersion


WORKDIR /humio-ingest-load-test-build
COPY . /humio-ingest-load-test-build/
RUN make && \
mkdir /humio-ingest-load-test \
&& cp /humio-ingest-load-test-build/target/scala-2.12/perftest.jar /humio-ingest-load-test/ \
&& cp /humio-ingest-load-test-build/entrypoint.sh /humio-ingest-load-test/ \
&& cp -R /humio-ingest-load-test-build/templates /humio-ingest-load-test/templates \
&& rm -rf /humio-ingest-load-test-build \
&& rm -rf /home/sbtuser \
&& rm -rf /root/.sbt /root/.ivy2 /root/.bashrc /root/.profile /root/.sbt /root/.cache /root/.wget-hsts
WORKDIR /humio-ingest-load-test
RUN rm -rf /humio-perf-test-build \
&& chmod +x /humio-ingest-load-test/entrypoint.sh

ENTRYPOINT ["/humio-ingest-load-test/entrypoint.sh"]
