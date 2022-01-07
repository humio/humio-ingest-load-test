FROM hseeberger/scala-sbt:8u312_1.5.8_2.13.7

RUN apt-get update  && apt-get install -y \
    build-essential \
    make \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /humio-ingest-load-test-build
COPY . /humio-ingest-load-test-build/
RUN make
RUN mkdir /humio-ingest-load-test
RUN cp /humio-ingest-load-test-build/target/scala-2.12/perftest.jar /humio-ingest-load-test/
RUN cp /humio-ingest-load-test-build/entrypoint.sh /humio-ingest-load-test/
WORKDIR /humio-ingest-load-test
RUN rm -rf /humio-perf-test-build
RUN chmod +x /humio-ingest-load-test/entrypoint.sh

ENTRYPOINT ["/humio-ingest-load-test/entrypoint.sh"]
