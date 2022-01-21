# vpcflow.ssp

This Readme describes the [vpcflow.ssp](vpcflow.ssp) template file found in this folder including what it does, how it works, and how you can customize the templates for your needs.

# What does it do?

Vpcflow.ssp generates simulated events that mimic AWS VPC Flow logs (https://docs.aws.amazon.com/vpc/latest/userguide/flow-logs.html) and look like the following examples:

```
2 123456789010 eni-123456789abc22367 156.237.46.147 30.26.157.88 9094 80 - 3652 52717 1642098500 1642098501 ACCEPT OK
2 123456789040 eni-123456789abc12345 - - - - - - - 1642098499 1642098501 - NODATA
2 123456789030 eni-123456789abc12345 - - - - - - - 1642098446 1642098448 - SKIPDATA
2 123456789020 eni-123456789abc52323 117.31.140.23 234.224.164.162 9094 2181 - 5321 38157 1642097697 1642097698 ACCEPT OK
```

This event format is compatible with the vpcflow_raw parser that is part of the aws/vpcflow package in the Humio Marketplaces.

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
  -e TEMPLATE=templates/vpcflow.ssp \
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

* vpc-accountid.csv - A list of example AWS account IDs
* vpc-interfaceid.csv - A list of example interface IDs
* vpc-ports.csv - A list of example ports

Each of these CSV files is very simple only containing two fields: an ID for the row, and the row value. The vpc-interfaceid.csv file looks like the following:

```
1,eni-123456789abc12345
2,eni-123456789abc22367
3,eni-123456789abc32380
4,eni-123456789abc42301
5,eni-123456789abc52323
```

If you want to change the output values you can simply edit the interface ID strings, add new lines, or delete lines you don't wish to include.

Within the template you will see that the data in your CSV files gets loaded inside of the `init` block:

```
    if (init) {
    	// Load CSV reference files
    	data.register("vpcaccountCSV", new CSVSampler(data.distributions.uniform, "templates/data/vpc-accountid.csv"))
    	data.register("interfaceCSV", new CSVSampler(data.distributions.uniform, "templates/data/vpc-interfaceid.csv"))
        data.register("portsCSV", new CSVSampler(data.distributions.exponential, "templates/data/vpc-ports.csv"))
    	...
    }
```

After the `init` block is completed you can access the values in the CSV using code that looks like:

```
    val interfaceRow = data.sampleRow("interfaceCSV")
    val interfaceStr = interfaceRow(1)
```

The first line of the code above grabs the next row in the CSV file (based on a uniform distribution) and then the second line in the code takes the value in field 1 of the row (field 0 is the row's ID).

## Changing How Individual Variables Generate Values

The template also has variable fields that generate dynamic values at run time. These fields are initialized in the `init` block like the CSV files described above. The following example creates a simple array of values:

```
        data.register("action",
            new ArraySampler(
                data.distributions.exponential,
                Array("ACCEPT", "REJECT", "-")
            )
        )
```

After the `init` block you can access the value like:

```
data.sample("action")
```

The following code example creates a variable named `bytes` that will have a value between 1 and 100000:

```
data.register("bytes", new IntSampler(data.distributions.normal, 1, 100000))
```

For more information on how variables work in templates please refer to the project's [README](../README.md) for more resources.
