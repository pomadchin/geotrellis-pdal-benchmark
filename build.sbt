name := "pdal-jni"
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

resolvers += Resolver.bintrayRepo("daunnc", "maven")

fork := true

javaOptions += "-Djava.library.path=/usr/local/lib"

libraryDependencies ++= Seq(
  "io.pdal" %% "pdal" % "1.4.0-M1",
  "org.scalatest"  %% "scalatest" % "3.0.0"  % "test"
)
