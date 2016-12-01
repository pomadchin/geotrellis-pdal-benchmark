package com.azavea

import java.io.File

import io.pdal._
import geotrellis.spark.pointcloud.json._
import geotrellis.spark.io.hadoop.HadoopPointCloudRDD
import geotrellis.spark.io.kryo.KryoRegistrator
import geotrellis.vector.Extent
import org.apache.spark.serializer.KryoSerializer
import org.apache.spark._
import org.apache.hadoop.fs.Path
import spire.syntax.cfor._

object PackedPointCount {
  def time[T](msg: String)(f: => T): T = {
    val s = System.currentTimeMillis
    val result = f
    val e = System.currentTimeMillis
    val t = "%,d".format(e - s)
    println("=================Points Count=================")
    println(s"$msg (in $t ms)")
    println("==============================================")
    result
  }

  def main(args: Array[String]): Unit = {
    val input = new Path(args.head)
    val chunkPath = System.getProperty("user.dir") + "/chunks/"

    val conf = new SparkConf()
      .setIfMissing("spark.master", "local[*]")
      .setAppName("PointCloudCount")
      .set("spark.serializer", classOf[KryoSerializer].getName)
      .set("spark.kryo.registrator", classOf[KryoRegistrator].getName)

    implicit val sc = new SparkContext(conf)

    /*val start = System.currentTimeMillis

    val bytes = Files.readAllBytes(Paths.get(args.head))
    val localPath = new File("/tmp", "l1.las")
    val bos = new BufferedOutputStream(new FileOutputStream(localPath))
    Stream.continually(bos.write(bytes))
    bos.close()

    val pipeline = Pipeline(fileToPipelineJson(localPath).toString)

    // PDAL itself is not 100% threadsafe
    AnyRef.synchronized { pipeline.execute }

    val pointViewIterator = pipeline.pointViews()
    // conversion to list to load everything into JVM memory
    val packedPoints = pointViewIterator.map { pointView =>
      val packedPoint =
        pointView.getPointCloud(
          metadata = pipeline.getMetadata(),
          schema   = pipeline.getSchema()
        )

      pointView.dispose()
      packedPoint
    }

    val pointsCount = packedPoints.map { pointCloud =>
      var acc = 0l
      cfor(0)(_ < pointCloud.length, _ + 1) { i =>
        pointCloud.get(i)
        acc += 1
      }
      acc
    }.reduce(_ + _)

    val end = System.currentTimeMillis

    val time = "%,d".format(end - start)
    println("=================Points Count=================")
    println(s"pointsCount (in $time ms): ${pointsCount}")
    println("==============================================")*/

    /*val pointClouds =
      time("Read point clouds") {
        val pipeline = Pipeline(fileToPipelineJson(new java.io.File(args.head)).toString)
        pipeline.execute
        val pointViewIterator = pipeline.pointViews()
        val result =
          pointViewIterator.toList.map { pointView =>
            val pointCloud =
              pointView.getPointCloud(
                metadata = pipeline.getMetadata(),
                schema   = pipeline.getSchema()
              )

            pointView.dispose()
            pointCloud
          }.toIterator
        pointViewIterator.dispose()
        pipeline.dispose()
        result
      }

    val pointCloud = pointClouds.next

    val (extent, (min, max)) =
      time("Finding min and max heights") {
        var (xmin, xmax) = (Double.MaxValue, Double.MinValue)
        var (ymin, ymax) = (Double.MaxValue, Double.MinValue)
        var (zmin, zmax) = (Double.MaxValue, Double.MinValue)
        cfor(0)(_ < pointCloud.length, _ + 1) { i =>
          val x = pointCloud.getX(i)
          val y = pointCloud.getY(i)
          val z = pointCloud.getZ(i)

          if(x < xmin) xmin = x
          if(x > xmax) xmax = x
          if(y < ymin) ymin = y
          if(y > ymax) ymax = y
          if(z < zmin) zmin = z
          if(z > zmax) zmax = z
        }
        (Extent(xmin, ymin, xmax, ymax), (zmin, zmax))
      }

    println(s"MIN, MAX = ($min, $max)")*/


    /*val source = time("Read point clouds") { HadoopPointCloudRDD(input) }

    val result: List[(Extent, (Double, Double))] = time("Finding min and max heights") {
      source.mapPartitions {
        _.map { case (_, pointCloud) =>
          var (xmin, xmax) = (Double.MaxValue, Double.MinValue)
          var (ymin, ymax) = (Double.MaxValue, Double.MinValue)
          var (zmin, zmax) = (Double.MaxValue, Double.MinValue)
          cfor(0)(_ < pointCloud.length, _ + 1) { i =>
            val x = time("x") { pointCloud.getX(i) }
            val y = time("y") { pointCloud.getY(i) }
            val z = time("z") { pointCloud.getZ(i) }

            if (x < xmin) xmin = x
            if (x > xmax) xmax = x
            if (y < ymin) ymin = y
            if (y > ymax) ymax = y
            if (z < zmin) zmin = z
            if (z > zmax) zmax = z
          }
          (Extent(xmin, ymin, xmax, ymax), (zmin, zmax))
        }
      }.collect().toList
    }

    val (extent, (min, max)) = result.head

    println(s"MIN, MAX = ($min, $max)")*/

    //os.environ['PWD'], "chunk-temp")


    try {

      val source = HadoopPointCloudRDD(
        input,
        geotrellis.spark.io.hadoop.HadoopPointCloudRDD.Options(tmpDir = Some(chunkPath))
      ).flatMap(_._2)

      val start = System.currentTimeMillis
      val pointsCount = source.mapPartitions { _.map { pointCloud =>
        //val arr = new Array[Int](pointCloud.length)
        var acc = 0l
        cfor(0)(_ < pointCloud.length, _ + 1) { i =>
          pointCloud.get(i)
          //val s = System.currentTimeMillis
          //pointCloud.get(i)
          //val e = System.currentTimeMillis
          //val t = "%,d".format(e - s).toInt
          //arr(i) = t
          acc += 1
        }
        acc
        //arr
      } }.reduce(_ + _)
      val end = System.currentTimeMillis

      val time = "%,d".format(end - start)
      println("=================Points Count=================")
      println(s"pointsCount (in $time ms): ${pointsCount}")
      println("==============================================")
    } finally sc.stop()
  }
}
