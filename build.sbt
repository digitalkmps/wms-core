val pekkoVersion = "1.1.2"
val pekkoHttpVersion = "1.1.0"
val logbackVersion = "1.3.14"

lazy val buildSettings = Seq(
  organization := "digitalkpms",
  version := "0.0.1",
  name := "wms-core",
  publish / skip := true,
  scalaVersion := "3.3.4")

lazy val commonScalacOptions = Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlint",
  "-Ywarn-unused:imports",
  "-encoding", "UTF-8")

lazy val commonJavacOptions = Seq(
  "-Xlint:unchecked",
  "-Xlint:deprecation")

lazy val commonSettings = Seq(
  Compile / scalacOptions ++= commonScalacOptions,
  Compile / javacOptions ++= commonJavacOptions,
  run / javaOptions ++= Seq("-Xms128m", "-Xmx1024m"),
  run / fork := true,
  Global / cancelable := false,
  licenses := Seq(
    ("CC0", url("http://creativecommons.org/publicdomain/zero/1.0"))))

lazy val wmscore = project
  .in(file("wmscore"))
  .enablePlugins(  JavaAppPackaging, DockerPlugin)
  .settings(buildSettings)
  .settings(commonSettings)
  .settings(
    Compile / run / mainClass := Some("wmscore.Wms"),
    Docker / packageName := "wms",
    Docker / version := "0.1",
    //dockerExposedPorts ++= Seq(17345, 17355, 17356),
    libraryDependencies ++= Seq(
      "org.apache.pekko" %% "pekko-cluster-sharding-typed" % pekkoVersion,
      "org.apache.pekko" %% "pekko-serialization-jackson" % pekkoVersion,
      "org.apache.pekko" %% "pekko-distributed-data" % pekkoVersion,
      "org.apache.pekko" %% "pekko-slf4j" % pekkoVersion,
      "org.apache.pekko" %% "pekko-http" % pekkoHttpVersion,
      "org.apache.pekko" %% "pekko-http-spray-json" % pekkoHttpVersion,
      "ch.qos.logback" % "logback-classic" % logbackVersion))


// Startup aliases for the first two seed nodes and a third, more can be started.
addCommandAlias("sharding1", "wmscore/runMain wmscore.Wms 7345")
addCommandAlias("sharding2", "wmscore/runMain wmscore.Wms 7355")
addCommandAlias("sharding3", "wmscore/runMain wmscore.Wms 7356")
