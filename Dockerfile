FROM hseeberger/scala-sbt

RUN apt-get update  && apt-get install -y \
    build-essential \
    make

WORKDIR /humio-ingest-load-test-build
COPY . /humio-ingest-load-test-build/
RUN make
RUN mkdir /humio-ingest-load-test
RUN cp /humio-ingest-load-test-build/target/scala-2.12/perftest.jar /humio-ingest-load-test/
WORKDIR /humio-ingest-load-test
RUN rm -rf /humio-perf-test-build

#env variables required PERF_USERS PERF_BULK_SIZE PERF_DATASOURCES PERF_SIMULATION HUMIO_TOKEN HUMIO_BASE_URL
# silulation is either HECSimulation,FixedRateIngestSimulation, FileeatSimulation
CMD ["sh", "-c", "java -jar -Dusers=${PERF_USERS} -Ddatasources=${PERF_DATASOURCES} -Dbulksize${PERF_BULK_SIZE} -Dtoken=${HUMIO_TOKEN} -Dbaseurls=${HUMIO_BASE_URL} perftest.jar -s com.humio.perftest.${PERF_SIMULATION}"]

