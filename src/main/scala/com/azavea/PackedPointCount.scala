package com.azavea

import geotrellis.spark.io.hadoop.HadoopPackedPointsRDD
import geotrellis.spark.io.kryo.KryoRegistrator

import org.apache.spark.serializer.KryoSerializer
import org.apache.spark._
import org.apache.hadoop.fs.Path
import spire.syntax.cfor._

object PackedPointCount {
  def main(args: Array[String]): Unit = {
    val input = new Path(args.head)

    val conf = new SparkConf()
      .setIfMissing("spark.master", "local[*]")
      .setAppName("PackedPointCount")
      .set("spark.serializer", classOf[KryoSerializer].getName)
      .set("spark.kryo.registrator", classOf[KryoRegistrator].getName)

    implicit val sc = new SparkContext(conf)

    try {
      val source = HadoopPackedPointsRDD(input)

      val start = System.currentTimeMillis
      val pointsCount = source.mapPartitions { _.map { case (_, packedPoints) =>
        var acc = 0l
        cfor(0)(_ < packedPoints.length, _ + 1) { i =>
          packedPoints.get(i)
          acc += 1
        }
        acc
      } }.reduce(_ + _)
      val end = System.currentTimeMillis

      val time = "%,d".format(end - start)
      println("=================Points Count=================")
      println(s"pointsCount (in $time ms): ${pointsCount}")
      println("==============================================")
    } finally sc.stop()
  }
}
