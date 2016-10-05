# EpTO

This is a working implementation of EpTO[[1]](http://dl.acm.org/citation.cfm?id=2814804) in Kotlin.

# Requirements & Running

## Requirements
* Docker >= 1.12
* OpenJDK or OracleJDK >= 8
* Read the [Cluster instructions](https://github.com/jocelynthode/epto-neem/blob/master/cluster_instructions.md)

## Running
If you want to run EpTO localy execute: `./run_benchmarks_local.sh`

If you want to run it on your cluster after having it setup: `./setup_cluster.sh` and on the master `./run_benchmarks_cluster.sh`

# Verification Scripts

Scripts are provided to verify the Ordering of EpTO and the PSS health. They are located in the folder results
