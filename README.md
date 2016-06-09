# EpTO

This is a working implementation of EpTO[[1]](http://dl.acm.org/citation.cfm?id=2814804) in Kotlin.

# Compiling & Running

To have a working jar just execute `./gradlew --daemon shadowJar`.

To then run the application just execute `java -cp epto-1.0-SNAPSHOT-all.jar epto.utilities.App` in the same folder as the jar.


# Scripts

A small ruby script is in `scripts/`. You can test the implementation locally with this script.

# Docker

For running epto peers on docker, you just need to run `scripts/epto-benchmark.sh N`, where N is the number of epto-peers you want to create. This script will create an image for epto, and create N containers each running one epto peer.<br /> After running the the scripts, the output of all epto peers should be at `scripts/` folder.
