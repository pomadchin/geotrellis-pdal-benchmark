# [PDAL Java bindings benchmark](https://github.com/PDAL/PDAL/pull/1371)

[![Join the chat at https://gitter.im/PDAL/PDAL](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/PDAL/PDAL?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## Build

### PDAL Build (example in the following Dockerfiles)
 * https://github.com/PDAL/PDAL/blob/master/scripts/docker/Dockerfile (all deps for PDAL)
 * https://github.com/PDAL/PDAL/blob/master/scripts/docker/dependencies/Dockerfile (PDAL build itself)
 * PDAL Branch to install: https://github.com/pomadchin/PDAL/tree/feature/pdal-jni

### PDAL JNI Binaries build
 * README.md in the following directory: https://github.com/pomadchin/PDAL/tree/feature/pdal-jni/java
 * You can use [this](https://github.com/pomadchin/geotrellis-pdal-benchmark/blob/master/dist/libpdaljni0.so) binary to place it into your `java.library.path` (example: `/usr/lib/jni/`).

### This project build
 * `./sbt assembly`
 * You can use [this](https://github.com/pomadchin/geotrellis-pdal-benchmark/blob/master/dist/geotrellis-pdal-assembly-0.1.0-SNAPSHOT.jar) assembly jar to run job.
 * As the only argument [com.azavea.PackedPointCount](https://github.com/pomadchin/geotrellis-pdal-benchmark/blob/master/src/main/scala/com/azavea/PackedPointCount.scala) accepts an HDFS path to files.


## Run

After everything installed on all Spark nodes, run your job:

```bash
spark-submit \
  --conf spark.driver.extraJavaOptions="-Djava.library.path=/usr/lib/jni/" \
  --class com.azavea.PackedPointCount \
  geotrellis-pdal-assembly-0.1.0-SNAPSHOT.jar hdfs://{PATH}/
```

## Motivation

* We want to use JNI bindings before PDAL 1.4 release. Original issue: https://github.com/PDAL/PDAL/pull/1371
