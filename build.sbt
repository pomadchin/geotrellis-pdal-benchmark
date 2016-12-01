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
  "LocationTech GeoTrellis Releases" at "https://repo.locationtech.org/content/repositories/geotrellis-releases"
  //"LocationTech GeoTrellis Snapshots" at "https://repo.locationtech.org/content/repositories/geotrellis-snapshots"
)

fork := true

javaOptions += "-Djava.library.path=/usr/local/lib"

val gtVersion = "1.1.0-PC-SNAPSHOT"

val geotrellis = Seq(
  "org.locationtech.geotrellis" %% "geotrellis-spark"      % gtVersion,
  "org.locationtech.geotrellis" %% "geotrellis-s3"         % gtVersion,
  "org.locationtech.geotrellis" %% "geotrellis-pointcloud" % gtVersion
)

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "2.0.0" % "provided",
  "org.scalatest"    %% "scalatest"  % "3.0.0" % "test"
) ++ geotrellis

test in assembly := {}

assemblyMergeStrategy in assembly := {
  case "reference.conf" | "application.conf" => MergeStrategy.concat
  case "META-INF/MANIFEST.MF" | "META-INF\\MANIFEST.MF" => MergeStrategy.discard
  case "META-INF/ECLIPSEF.RSA" | "META-INF/ECLIPSEF.SF" => MergeStrategy.discard
  case _ => MergeStrategy.first
}
