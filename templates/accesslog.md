# accesslog.ssp

This Readme describes the [accesslog.ssp](accesslog.ssp) template file found in this folder including what it does, how it works, and how you can customize the templates for your needs.

# What does it do?

Accesslog.ssp generates simulated accesslog events that look like the following examples:

```
83.39.139.4 - jerome [12/Jan/2022:15:08:09 +0000] "GET /account.html HTTP/1.1" 200  12095 "https://duckduckgo.org/" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_2) AppleWebKit/537.36 (KHTML"
133.52.161.108 - veritas [12/Jan/2022:15:08:09 +0000] "GET /account.html HTTP/1.1" 204  2372 "https://www.google.com/search?q=logging" "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:27.0) Gecko/20100101 Firefox/27.0"
14.102.212.228 - emmy [12/Jan/2022:15:08:09 +0000] "GET /products/electronics/1966 HTTP/1.1" 200  560 "https://www.bro.org/" "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:27.0) Gecko/20100101 Firefox/27.0"
9.76.249.13 - percy [12/Jan/2022:15:08:09 +0000] "GET /products/clothes/1220 HTTP/1.1" 200  2953 "https://nowhere.com/" "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:31.0) Gecko/20100101 Firefox/31.0"
```

This event format is compatible with the accesslog parser that is built into Humio.

To use this template when running the humio-ingest-load-test tool you would use the following command (replacing the appropriate values for your environment):

```
docker run \
  -v /<local-path-to-cloned-repository>/humio-ingest-load-test/templates/humio-ingest-load-test/templates \
  -e PERF_TIME=300 \
  -e PERF_USERS=1000 \
  -e PERF_DATASOURCES=50 \
  -e PERF_BULK_SIZE=1000 \
  -e HUMIO_TOKEN=<humio-ingest-token> \
  -e HUMIO_BASE_URL=https://<humio-url> \
  -e PERF_SIMULATION=HECTemplateSimulation \
  -e TEMPLATE=templates/accesslog.ssp \
  humio/humio-ingest-load-test:latest
```

# How does it work?

**Note**: If you are completely new to SSP templates please start with the project's [README](../README.md) to get started with the basics of how and why the Humio ingest load-testing tool is using SSP templates.

# How can you customize it?

Assuming that you do not want to change the format of the events being written, just the values that are written to the events, there are two ways to change the output:

* Change the contents of the CSV files that the template uses;
* or change the way that the variables in the code are set.


## Editing the CSV Files

These easiest way to change the output is to change to values contained in the four CSV files that accesslog.ssp uses:

* referrer.csv - Example urls of to write to the referrer field
* urls.csv - Example urls on the web server that users are visiting
* useragent.csv - Example user agent strings
* userid.csv - Example user IDs

**Note**: There is code in the template file that looks for strings that start with `/products` or `/blog` and adds a generated number to the end of the string to simulate users navigating to a number of different pages (thousands potentially). If you change the format of the urls in the CSV file you might need to update the code in the template so that it will continue to work.

Each of these CSV files is very simple only containing two fields: an ID for the row, and the row value. The urls.csv file looks like the following:

```
1,/index.html
2,/support.html
3,/faqs.html
4,/account.html
5,/careers.html
6,/music.html
7,/products/books/
8,/products/electronics/
9,/products/clothes/
10,/products/food/
11,/products/toys/
12,/products/bicycles/
13,/blog/posts/
```

If you want to change the output values you can simply edit the url strings, add new lines, or delete lines you don't wish to include.

Within the template you will see that the data in your CSV files gets loaded inside of the `init` block:

```
    if (init) {
    	// Load CSV reference files
    	data.register("referrerCSV", new CSVSampler(data.distributions.uniform, "templates/data/referrer.csv"))
    	data.register("agentCSV", new CSVSampler(data.distributions.uniform, "templates/data/useragent.csv"))
    	data.register("userCSV", new CSVSampler(data.distributions.uniform, "templates/data/userid.csv"))
    	data.register("urlCSV", new CSVSampler(data.distributions.uniform, "templates/data/urls.csv"))
    	...
    }
```

After the `init` block is completed you can access the values in the CSV using code that looks like:

```
    val userRow = data.sampleRow("userCSV")
    val userStr =  userRow(1)
```

The first line of the code above grabs the next row in the CSV file (based on a uniform distribution) and then the second line in the code takes the value in field 1 of the row (field 0 is the row's ID).

## Changing How Individual Variables Generate Values

The template also has variable fields that generate dynamic values at run time. These fields are initialized in the `init` block like the CSV files described above. The following example creates a simple array of values:

```
        data.register("method",
            new ArraySampler(
                data.distributions.exponential,
                Array("GET", "POST", "PUT", "DELETE", "OPTION")
            )
        )
```

After the `init` block you can access the value like:

```
data.sample("method")
```

The following code example creates a variable named `productid` that will have a value between 1000 and 2500:

```
data.register("productid", new IntSampler(data.distributions.uniform, 1000, 2500))
```

For more information on how variables work in templates please refer to the project's [README](../README.md) for more resources.
