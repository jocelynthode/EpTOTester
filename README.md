# EpTO

This is a working implementation of EpTO[[1]](http://dl.acm.org/citation.cfm?id=2814804) in Kotlin.

# Requirements & Running

## Requirements
* Docker >= 1.12
* OpenJDK or OracleJDK >= 8
* Read the [Cluster instructions](https://github.com/jocelynthode/epto-neem/blob/master/cluster_instructions.md)

## Running
If you want to run EpTO localy execute: `cluster/run_benchmarks.py` with the `--local` option on

If you want to run it on your cluster follow the [Cluster instructions](https://github.com/jocelynthode/epto-neem/blob/master/cluster_instructions.md)

If you only want to obtain and run the Java program. Gradle with shadowJar is used to generate a jar file.

# Verification Scripts

Scripts are provided to verify the ordering of EpTO, extract various informations from EpTO logs and the PSS health. They are located in the folder results.

# Churn Generation

## FTA 
The [Failure Trace Archive](http://fta.scem.uws.edu.au) regroups various failure traces. These traces can be exploited by the python classes `cluster/nodes_trace.py` and `cluster/churn.py`.

## Synthetic 
The above classes can also be used to generate simple synthethic churn. To find out how to use them, please refer to the help of `cluster/run_benchmarks.py`
