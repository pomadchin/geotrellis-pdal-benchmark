name := "geotrellis-pdal"
version := "0.1.0-SNAPSHOT"
scalaVersion := "2.11.8"
crossScalaVersions := Seq("2.12.0", "2.11.8")
organization := "com.azavea"
scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-Yinline-warnings",
  "-language:implicitConversions",
  "-language:reflectiveCalls",
  "-language:higherKinds",
  "-language:postfixOps",
  "-language:existentials",
  "-feature")

resolvers ++= Seq(
  Resolver.bintrayRepo("daunnc", "maven"),
  "LocationTech GeoTrellis Releases" at "https://repo.locationtech.org/content/repositories/geotrellis-releases",
  "LocationTech GeoTrellis Snapshots" at "https://repo.locationtech.org/content/repositories/geotrellis-snapshots"
)

fork := true

javaOptions += "-Djava.library.path=/usr/local/lib"

val gtVersion = "1.0.0-SNAPSHOT"

val geotrellis = Seq(
  "org.locationtech.geotrellis" %% "geotrellis-spark" % gtVersion,
  "org.locationtech.geotrellis" %% "geotrellis-s3"    % gtVersion,
  "org.locationtech.geotrellis" %% "geotrellis-pdal"  % gtVersion
)

libraryDependencies ++= Seq(
  "io.pdal" %% "pdal" % "1.4.0-M1",
  "org.scalatest"  %% "scalatest" % "3.0.0"  % "test"
) ++ geotrellis
