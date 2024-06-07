<!--
Copyright (c) 2017 YCSB contributors. All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License. You
may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License. See accompanying
LICENSE file.

NOTE: borrowed from cloudspanner/
-->

# witnesskvs Driver for YCSB

This driver provides a YCSB workload binding for witnesskvs. This binding is implemented using the unofficial Java client library for witnesskvs which uses GRPC for making calls.

# building

~/.local/bin/mvn -Dwitnesskvpath=/path/to/src/witness-kv -pl site.ycsb:witnesskvs-binding -am package

## Running a Workload

We recommend reading the [general guidelines](https://github.com/brianfrankcooper/YCSB/wiki/Running-a-Workload) in the YCSB documentation, and following the witnesskvs specific steps below.

### 1. Set up witnesskvs

Setup the nodes according to instructions.

Make note of your IP Addresses and Ports.

### 2. Edit Properties

In your YCSB root directory, edit `witnesskvs/conf/witnesskvs.properties` and specify your nodes and ports.

### 4. Run the YCSB Shell

Start the YCBS shell connected to witnesskvs using the following command:

```
export witnesskvpath=/path/to/src/witness-kv
./bin/ycsb shell witnesskvs -P witnesskvs/conf/witnesskvs.properties
```

You can use the `get`, `put`, and `delete` commands in the shell to experiment with your witnesskvs and make sure the connection works. For example, try the following:

```
put a b
read a
delete a
```

### 5. Load the Data

You can load, say, 10 GB of data into your YCSB database using the following command:

```
./bin/ycsb load witnesskvs -P witnesskvs/conf/witnesskvs.properties -P workloads/workloada -p recordcount=10000000 -threads 10 -s
```

If you wish to load a large database, you can run YCSB on multiple client VMs in parallel and use the `insertstart` and `insertcount` parameters to distribute the load as described [here](https://github.com/brianfrankcooper/YCSB/wiki/Running-a-Workload-in-Parallel). In this case, we recommend the following:

* Use ordered inserts via specifying the YCSB parameter `insertorder=ordered`;
* Use zero-padding so that ordered inserts are actually lexicographically ordered; the option `zeropadding = 12` is set in the default `witnesskvs.properties` file;
* Split the key range evenly between client VMs;
* Use few threads on each client VM, so that each individual commit request contains keys which are (close to) consecutive, and would thus likely address a single split; this also helps avoid overloading the servers.

### 6. Run a Workload

After data load, you can a run a workload, say, workload B, using the following command:

```
./bin/ycsb run witnesskvs -P witnesskvs/conf/witnesskvs.properties -P workloads/workloadb -p recordcount=10000000 -p operationcount=1000000 -threads 10 -s 
```

Make sure that you use the same `insertorder` (i.e. `ordered` or `hashed`) and `zeropadding` as specified during the data load. Further details about running workloads are given in the [YCSB wiki pages](https://github.com/brianfrankcooper/YCSB/wiki/Running-a-Workload).

## Configuration Options

In addition to the standard YCSB parameters, the following Cloud Spanner specific options can be configured using the `-p` parameter or in `witnesskvs/conf/witnesskvs.properties`.

* `witnesskvs.nodes`: (Required) A comma separated list of nodes and ports.
