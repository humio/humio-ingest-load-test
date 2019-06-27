# Humio ingest load-testing tool #

### Requirements for running Humio server on your local machine ###

* Java 9 or later
* sbt
* make

## Building

Execute `sbt assembly` or just plain `make`

## Running

Copy the resulting executable `target/scala-2.12/perftest.jar` to some
machine close to the server(s) to test, then execute something like
this, perhaps in multiple copies in parallel, as the users parameter
does not add load linearly for higher numbers. Replace the
test.EXAMPLE.COM and token string with relevant name and token for your
server to be tested. You may need to run the test client on multiple
machines in order to generate more traffic than one network link can
handle, if that is part of the test.

```
java -Dbulksize=1000 -Ddatasources=50 -Dbaseurls=https://test.EXAMPLE.COM  -Dusers=100 -Dtoken=<SOME-INGEST-TOKEN>  -jar ./perftest.jar -s com.humio.perftest.HECSimulation

java -Dbulksize=1000 -Ddatasources=50 -Dbaseurls=https://test.EXAMPLE.COM  -Dusers=100 -Dtoken=<SOME-INGEST-TOKEN>  -jar ./perftest.jar -s com.humio.perftest.FilebeatSimulation
```

### Running with Docker

For HECSimulation the URL should be `$BASEURL/api/v1/ingest/hec`.

For the FilebeatSimulation and FixedRateIngestSimulation the URL should be `$BASEURL/api/v1/ingest/elastic-bulk`.

`$BASEURL` should be the URL containing the Humio host. For example https://cloud.humio.com.

#### FixedRateIngestSimulation
```
docker run \
  -e PERF_TIME=300 \
  -e PERF_TENS_GB_PER_DAY=10 \
  -e HUMIO_TOKEN=<SOME-INGEST-TOKEN> \
  -e HUMIO_BASE_URL=<URL to Humio ingest endpoint> \
  -e PERF_SIMULATION=FixedRateIngestSimulation \
  humio/humio-ingest-load-test:latest
```

#### HECSimulation or FilebeatSimulation
```
docker run \
  -e PERF_TIME=300
  -e PERF_USERS=1000 \
  -e PERF_DATASOURCES=50 \
  -e PERF_BULK_SIZE=1000 \
  -e HUMIO_TOKEN=<SOME-INGEST-TOKEN> \
  -e HUMIO_BASE_URL=<URL to Humio ingest endpoint> \
  -e PERF_SIMULATION=<HECSimulation or FilebeatSimulation> \
  humio/humio-ingest-load-test:latest
```

#### QuerySimulation
```
docker run \
  -e PERF_SEARCH_QUERY="count()" \
  -e PERF_SEARCH_DURATION=24hours \
  -e HUMIO_TOKEN=<SOME-INGEST-TOKEN> \
  -e HUMIO_BASE_URL=<URL to Humio> \
  -e PERF_SIMULATION=QuerySimulation \
  humio/humio-ingest-load-test:latest
```

## License
[Apache License, Version 2](http://www.apache.org/licenses/LICENSE-2.0.txt)
