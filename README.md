# Humio ingest load-testing tool #

### Requirements for running Humio server on your local machine ###

* Java 9 or later
* sbt
* make

## Building

Execute `sbt assembly` or just plain `make`

## Running

Copy the resulting executable `target/scala-2.13/perftest.jar` to some
machine close to the server(s) to test, then execute something like
this, perhaps in multiple copies in parallel, as the users parameter
does not add load linearly for higher numbers. Replace the
test.EXAMPLE.COM and token string with relevant name and token for your
server to be tested. You may need to run the test client on multiple
machines in order to generate more traffic than one network link can
handle, if that is part of the test.

```
java -Dbulksize=1000 -Ddatasources=50 -Dbaseurls=https://test.EXAMPLE.COM -Dusers=100 -Dtoken=<SOME-INGEST-TOKEN>  -jar ./perftest.jar -s com.humio.perftest.HECSimulation

java -Dbulksize=1000 -Ddatasources=50 -Dbaseurls=https://test.EXAMPLE.COM -Dusers=100 -Dtoken=<SOME-INGEST-TOKEN> -Dtemplate=templates/test.ssp -jar ./perftest.jar -s com.humio.perftest.HECSimulation

java -Dbulksize=1000 -Ddatasources=50 -Dbaseurls=https://test.EXAMPLE.COM -Dusers=100 -Drandomness=3 -Dtoken=<SOME-INGEST-TOKEN>  -jar ./perftest.jar -s com.humio.perftest.HECRandomnessSimulation

java -Dbulksize=1000 -Ddatasources=50 -Dbaseurls=https://test.EXAMPLE.COM -Dusers=100 -Dtoken=<SOME-INGEST-TOKEN>  -jar ./perftest.jar -s com.humio.perftest.FilebeatSimulation
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
  -e PERF_TIME=300 \
  -e PERF_USERS=1000 \
  -e PERF_DATASOURCES=50 \
  -e PERF_BULK_SIZE=1000 \
  -e HUMIO_TOKEN=<SOME-INGEST-TOKEN> \
  -e HUMIO_BASE_URL=<URL to Humio ingest endpoint> \
  -e PERF_SIMULATION=<HECSimulation or FilebeatSimulation> \
  humio/humio-ingest-load-test:latest
```

#### HECRandomnessSimulation
```
docker run \
  -e PERF_TIME=300 \
  -e PERF_USERS=1000 \
  -e PERF_DATASOURCES=50 \
  -e PERF_BULK_SIZE=1000 \
  -e HUMIO_TOKEN=<SOME-INGEST-TOKEN> \
  -e HUMIO_BASE_URL=<URL to Humio ingest endpoint> \
  -e PERF_SIMULATION=HECRandomnessSimulation \
  -e RANDOMNESS=3 \
  humio/humio-ingest-load-test:latest
```

#### HECTemplateSimulation
```
docker run \
  -e PERF_TIME=300 \
  -e PERF_USERS=1000 \
  -e PERF_DATASOURCES=50 \
  -e PERF_BULK_SIZE=1000 \
  -e HUMIO_TOKEN=<SOME-INGEST-TOKEN> \
  -e HUMIO_BASE_URL=<URL to Humio ingest endpoint> \
  -e PERF_SIMULATION=HECTemplateSimulation \
  -e TEMPLATE=templates/test.ssp \
  humio/humio-ingest-load-test:latest

# If you want to use a custom templates and data you need to mount a local volume into the container.
docker run \
  -e PERF_TIME=300 \
  -e PERF_USERS=1000 \
  -e PERF_DATASOURCES=50 \
  -e PERF_BULK_SIZE=1000 \
  -e HUMIO_TOKEN=<SOME-INGEST-TOKEN> \
  -e HUMIO_BASE_URL=<URL to Humio ingest endpoint> \
  -e PERF_SIMULATION=HECTemplateSimulation \
  -e TEMPLATE=templates/my-template.ssp> \
  -v /Users/me/humio-load-test-templates:/humio-ingest-load-test/templates/my-template.ssp> \
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

## HECTemplateSimulation

The goal of HECTemplateSimulation is to make dynamic content generation easier, as well as generate data that is realistically compressible and, ideally, quasi-meaningfully queryable for demonstration purposes.

Toward this end, `HECTemplateSimulation` provides the ability to use a specified `.ssp` file, interpreted by the [Scalate](https://scalate.github.io/scalate/) engine; in this specific instance, the [_SSP_ implementation](https://scalate.github.io/scalate/documentation/ssp-reference.html) is used, which is similar to Velocity, JSP, Erb, etc.

Along with the ability to template, `HECTemplateSimulation` adds the ability to generate different types of data according to a specified distribution (examples below).

### Templating

#### Implementation

`SSP` (so-called "Scala Server Pages") directives are documented in full [here](https://scalate.github.io/scalate/documentation/ssp-reference.html).

Below I will walk you through the different sections, and mechanics, of template handling.  See the file `templates/test.ssp` for a full working example.

#### Template: Imports

Templates require a number of imports to function correctly.  More may be required if you implement custom functionality beyond that supported by these typical libraries:

```
import com.humio.perftest._
import play.api.libs.json._
import org.fusesource.scalate._
import org.fusesource.scalate.RenderContext._
```

#### Template: Init Block

Templates typically use an _init block_ to create and register data generators and one-time values.  This block of the template is only run once upon initialization.  It is implemented very simply, switched on a context variable `init`:

```
    if (init) {

        data.register("httpMethod",
            new ArraySampler(
                data.distributions.exponential,
                Array("GET", "POST", "PUT", "DELETE", "OPTION")
            )
        )

		// .... 

	}
```

#### Template: Generation

All code outside the _init_ block between `<%` and `%>` (see the `SSP` & Scalate reference docs for more information on this and other template directives) is executed on every data generation request.  This is the section where the actual data generation from the template takes place.  For example, with the `templates/test.ssp` example, this section is outside the _init_ block and is executed on every generation call:

```
    val sourceType = "applog"
    val logTs = data.timestamp
    val tag = "tag" + data.sample("tagNum")
    val source = "file" + data.sample("sourceNum")
    val viewId = data.sha1(data.sample("viewAndQueryId")).substring(0,10)
    val queryId = "IQ-" + viewId.substring(0,5).reverse + viewId.substring(5).reverse
    var logLine: String = capture {
%>
${logTs} requests 11 - route=humio method=<%= data.sample("httpMethod") %>, remote=10.0.<%= data.sample("host1") %>.<%= data.sample("host2") %>:<%= data.sample("port") %> uri=http://newcloud<%= data.sample("cloudHost") %>:8080/api/v1/internal/views/${viewId}/queryjob/${queryId}, status=<%= data.sample("status") %>, time=<%= data.sample("time") %>, internal=<%= data.sample("internal") %>, contentLength=<%= data.sample("contentLength") %>
<%
    }.trim // capture
    val jsonObj = Json.obj(
        "source" -> source,
        "sourcetype" -> sourceType,
        "event" -> logLine,
        "time" -> logTs,
        "fields" -> Json.obj(
            "host" -> "perftesthost",
            "source2" -> source
        )
    )
    val output = Json.toJson(jsonObj)
%>${output.toString.trim}
```

### Data Generation

#### Introduction

Simple data generation methods provide things like timestamps, etc.  Sampled data generators must be created and registered.  Their function is to select from sets of elements with specified probabilities.

#### Simple Data Generation

There are a handful of functions available to generate simple types of data:

| Name | Implementation |
| ---- | -------------- |
| `data.timestamp` | Current timestamp, via `"%.6f".format(ZonedDateTime.now().toInstant.toEpochMilli.toDouble / 1000d)` |
| `data.iso8601Timestamp` | Current timestamp, via `SimpleDateFormat` format `yyyy-MM-dd'T'HH:mm'Z'` |
| `data.sha1(String)` | SHA1 hash of given string.  Useful for generating identifiers from other data types. |

#### Samplers: Creating and Registering

For example, let's say you have a log line you would like to generate similar log lines of:

```
2021-01-01T0:0:0.0Z requests 11 - route=humio method=GET, remote=10.0.1.10, ...
```

We want to replace the values for the `method` and `remote` key-value pairs with data that is plausible.  The first step is to think about how the log line works: "What is the distribution of HTTP request methods for this endpoint?".  For this example, let's say it's predominantly `GET`, with `POST` a distant second, and the rest typically representative of error or malformed calls.  For the purposes of this example, let's say it's best represented by an exponential distribution.  Highly accurate distribution estimation is not generally necessary to achieve the aims of this project and its methods are beyond the scope of this document.

So, for `method`, to sample from an array of values with exponentially decreasing probability (more later on his this mapping is achieved), you can create an `ArraySampler` and register it with the name `httpMethod`:

```
data.register("httpMethod",
    new ArraySampler(
        data.distributions.exponential,
        Array("GET", "POST", "PUT", "DELETE", "OPTION")
    )
)
```

The above code creates an `ArraySampler` that maps exponentially decreasing probabilities to successive elements in the provided array.

Now, the `remote` element.  To make this more interesting to query when combined with other generated elements, let's break the field into 10.0._host1_._host2_, where the distributions of _host1_ and _host2_, while different, combine to form a useful structure of IP addresses in the aggregate.  For example:

```
data.register("host1", new IntSampler(data.distributions.exponential, 0, 10))
data.register("host2", new IntSampler(data.distributions.uniform, 0, 20))
```

The first generator, `host1`, is defined as a sampler over the integer range 0 to 10 with decreasing exponential probability (i.e., 0 is most likely, 10 least).  The second generator, `host2`, is defined as a sampler over the integer range 0 to 20, with uniform probability.

The available samplers and distributions are described in complete below.

#### Samplers: Sampling

Once a sampler (e.g., `ArraySampler`) has been created and registered, it can be sampled from to produce a value.  Following along with the `templates/test.ssp` example, the example log line we're trying to create a generator for becomes the following:

```
${logTs} requests 11 - route=humio method=<%= data.sample("httpMethod") %>, remote=10.0.<%= data.sample("host1") %>.<%= data.sample("host2") %>, ...
```

The relevant part in this context is this: `<$= data.sample("httpMethod") %>`.  The `<%=` and `%>` tags say "execute the code within this block and output the value.  The `data.sample("httpMethod")` code requests a sample from the sampler registered under the name `httpMethod`, which we created and registered above.  The same concept applies to the _host1_ and _host2_ samplers.

Note: `CSVSampler` is a sampler that exposes a `sampleRow` function as well as a `sample` function.  `sampleRow` samples a row of values from the provided CSV file that can then be access by index, e.g., `someVar(1)`.  Using `sample` with a `CSVSampler` simply returns the value in the first column of the sampled row.

#### Samplers: Built-ins

| Name | Description | Signature |
| ---- | ----------- | ------- |
| ArraySampler | Samples from an array of Strings. | `new ArraySampler(<distribution>, Array("value", ...))` |
| IntSampler | Samples from an increasing range of integers. | `new IntSampler(<distribution>, <lower>, <upper>)` |
| RealSampler | Samples from an increasing range of doubles. | `new RealSampler(<distribution>, <lower>, <upper>)` |
| CSVSampler | Samples rows from a CSV file. | `new CSVSampler(<distribution>, "<csv filename>")` |
 
#### Distributions

##### Distributions: Method

Distributions are implemented with the [Apache Commons Math Library](https://commons.apache.org/proper/commons-math/userguide/distribution.html).  Since they are distributions over the reals, the following method was used to map probabilities (see `SimUtils` for further information):

```
abstract class Sampleable(distribution: RealDistribution) {
  val minProbability = 0.0001
  val maxProbability = 0.999999

  val icp0 = distribution.inverseCumulativeProbability(minProbability)
  val icp1 = distribution.inverseCumulativeProbability(maxProbability)
  val icpDif = icp1 - icp0

  def sampleDistribution = {
    val sample = distribution.sample()
    if (sample < icp0) 0
    else if (sample > icp1) 1
    else (sample - icp0) / icpDif
  }

  def sample: String
}
```

With the mapping following the pattern similar to:

```
class ArraySampler(distribution: RealDistribution, values:Array[String]) extends Sampleable(distribution = distribution) {
  override def sample: String = values(((values.length-1).toDouble * sampleDistribution).round.toInt)
}
```

The actual distribution sampling, `distribution.sample()`, is implemented by [Inverse transform sampling](https://en.wikipedia.org/wiki/Inverse_transform_sampling).

##### Distributions: Built-ins

Distribution specification has been simplified with default values, which are described below.  If you require a specific configuration, see `TemplateHelper.distributions` in `SimUtils`.

| Name | Description |
| ---- | ----------- |
| data.distributions.exponential | Exponential distribution, default _mean_=1 |
| data.distributions.normal | Normal distribution, default _mean_=0, _scale_=1 |
| data.distributions.uniform | Uniform distribution |
| data.distributions.logNormal | Log-normal distribution, default _scale_=0, _shape_=1 |

In a pinch, you can import `import org.apache.commons.math3.distribution._` in your template and construct a distribution directly if you don't want to add the specific distribution and associated default parameters to the default options, e.g.,

```
data.register("contentLength", 
	new IntSampler(
		new BetaDistribution(0.5, 0.5), 
		0, 256000))
```

All available distributions are [subclasses of AbstractRealDistribution](https://commons.apache.org/proper/commons-math/javadocs/api-3.6.1/org/apache/commons/math3/distribution/AbstractRealDistribution.html).

##### Distributions: Random Number Generation

All distribution sampling uses the [`Well19937c`](https://commons.apache.org/proper/commons-math/javadocs/api-3.6.1/org/apache/commons/math3/random/Well19937c.html) generator.

### Tips

#### Correlated Values

Let's say you have a pair of fields being generated that must be correlated, e.g., a pair of fields named `rcode_name` and `rcode`, where they both hold a value that relates to the other.  A good way to do this is to use a `CSVSampler` that references a CSV file that maps one to the other, e.g.,

```
NOERROR,0
SERVFAIL,2
NXDOMAIN,3
REFUSED,5
```

With this input to the `CSVSampler`, along with an appropriate distribution, you can use `sampleRow`, e.g.,

```
// init:
data.register("rcode", new CSVSampler(data.distributions.exponential, "templates/data/dns_rcode.samples.csv"))

// sample:
val rcodeRow = data.sampleRow("rcode")
val rcodeName = rcodeRow(0)
val rcode = rcodeRow(1)
```

For a real-world example of this, see the `corelight-dns.ssp` template.

#### Simple Random Sampling (with replacement)

Let's say you have a variable that you're unsure how to model, but you _do_ have many observations of that variable (the "_sample frame_").  One method of sampling this variable is to load a CSV file with _all_ the observations -- do not process this list of values in any way! -- into a `CSVSampler` with a _uniform_ distribution, e.g.,

```
// init:
data.register("TTL", new CSVSampler(data.distributions.uniform, "templates/data/dns_ttls0.csv"))

// sample:
val ttl = data.sample("TTL")
```

The bigger the sample frame, the more the sampled data will be representative of the underlying distribution.  

_Notes_: 

* All `CSVSampler` data is stored in memory: be mindful and plan accordingly when using large sets of observations.
* While there are many opportunities for significant error when using this method, for simple data generation tasks it can still be useful.

### Utilities

#### `com.humio.perftest.TemplateTest`

There is a command-line tool to test templates.  It will generate data corresponding to 10 executions of the template.  Run `sbt`, then `runMain com.humio.perftest.TemplateTest <filename>`.  For example:

```
sbt:humio-ingest-load-test> runMain com.humio.perftest.TemplateTest templates/test.ssp

(...)

{"source":"file8","sourcetype":"applog","event":"1613578255.420000 requests 11 - route=humio method=GET, remote=10.0.0.12:33392 uri=http://newcloud8:8080/api/v1/internal/views/tlifxqsNyC/queryjob/IQ-xfiltCyNsq, status=200, time=0.07, internal=true, contentLength=3308","time":"1613578255.420000","fields":{"host":"perftesthost","source2":"file8"}}
{"source":"file11","sourcetype":"applog","event":"1613578255.574000 requests 11 - route=humio method=GET, remote=10.0.2.6:37436 uri=http://newcloud6:8080/api/v1/internal/views/tlifxqsNyC/queryjob/IQ-xfiltCyNsq, status=200, time=0.01, internal=true, contentLength=1336","time":"1613578255.574000","fields":{"host":"perftesthost","source2":"file11"}}
{"source":"file8","sourcetype":"applog","event":"1613578255.575000 requests 11 - route=humio method=GET, remote=10.0.0.6:43388 uri=http://newcloud7:8080/api/v1/internal/views/tlifxqsNyC/queryjob/IQ-xfiltCyNsq, status=200, time=0.17, internal=true, contentLength=5218","time":"1613578255.575000","fields":{"host":"perftesthost","source2":"file8"}}
{"source":"file7","sourcetype":"applog","event":"1613578255.575000 requests 11 - route=humio method=GET, remote=10.0.0.24:35596 uri=http://newcloud4:8080/api/v1/internal/views/tlifxqsNyC/queryjob/IQ-xfiltCyNsq, status=200, time=0.11, internal=true, contentLength=7093","time":"1613578255.575000","fields":{"host":"perftesthost","source2":"file7"}}
{"source":"file6","sourcetype":"applog","event":"1613578255.576000 requests 11 - route=humio method=POST, remote=10.0.0.5:24921 uri=http://newcloud1:8080/api/v1/internal/views/tlifxqsNyC/queryjob/IQ-xfiltCyNsq, status=200, time=0.02, internal=true, contentLength=3969","time":"1613578255.576000","fields":{"host":"perftesthost","source2":"file6"}}
{"source":"file11","sourcetype":"applog","event":"1613578255.576000 requests 11 - route=humio method=GET, remote=10.0.1.28:30394 uri=http://newcloud8:8080/api/v1/internal/views/NWoZK3kTsE/queryjob/IQ-KZoWNEsTk3, status=200, time=0.04, internal=true, contentLength=535","time":"1613578255.576000","fields":{"host":"perftesthost","source2":"file11"}}
{"source":"file7","sourcetype":"applog","event":"1613578255.577000 requests 11 - route=humio method=GET, remote=10.0.1.26:31617 uri=http://newcloud10:8080/api/v1/internal/views/tlifxqsNyC/queryjob/IQ-xfiltCyNsq, status=200, time=0.04, internal=true, contentLength=1699","time":"1613578255.577000","fields":{"host":"perftesthost","source2":"file7"}}
{"source":"file9","sourcetype":"applog","event":"1613578255.577000 requests 11 - route=humio method=POST, remote=10.0.0.44:56138 uri=http://newcloud5:8080/api/v1/internal/views/tlifxqsNyC/queryjob/IQ-xfiltCyNsq, status=200, time=0.13, internal=true, contentLength=3224","time":"1613578255.577000","fields":{"host":"perftesthost","source2":"file9"}}
{"source":"file7","sourcetype":"applog","event":"1613578255.578000 requests 11 - route=humio method=GET, remote=10.0.2.17:35097 uri=http://newcloud10:8080/api/v1/internal/views/tlifxqsNyC/queryjob/IQ-xfiltCyNsq, status=200, time=0.05, internal=true, contentLength=6346","time":"1613578255.578000","fields":{"host":"perftesthost","source2":"file7"}}
{"source":"file9","sourcetype":"applog","event":"1613578255.578000 requests 11 - route=humio method=GET, remote=10.0.0.12:56221 uri=http://newcloud11:8080/api/v1/internal/views/tlifxqsNyC/queryjob/IQ-xfiltCyNsq, status=200, time=0.11, internal=true, contentLength=5762","time":"1613578255.578000","fields":{"host":"perftesthost","source2":"file9"}}
{"source":"file4","sourcetype":"applog","event":"1613578255.579000 requests 11 - route=humio method=GET, remote=10.0.0.13:36705 uri=http://newcloud13:8080/api/v1/internal/views/tlifxqsNyC/queryjob/IQ-xfiltCyNsq, status=204, time=0.0, internal=true, contentLength=709","time":"1613578255.579000","fields":{"host":"perftesthost","source2":"file4"}}
```

### Sample Templates

This project includes sample templates that can be found in the templates directory: [templates](templates).

## License
[Apache License, Version 2](http://www.apache.org/licenses/LICENSE-2.0.txt)
