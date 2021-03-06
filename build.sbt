import com.typesafe.sbt.SbtMultiJvm
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.MultiJvm
 
val akkaVersion = "2.5.16"
val akkaHttpVersion = "10.1.5"

lazy val `blockparser` = project
  .in(file("."))
  .settings(SbtMultiJvm.multiJvmSettings: _*)
  .enablePlugins(JavaAppPackaging)
  .settings(
    organization := "com.wcrbrm.blockparser",
    scalaVersion := "2.12.6",
    scalacOptions in Compile ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlog-reflective-calls"), // "-Xlint"
    javacOptions in Compile ++= Seq("-Xlint:unchecked", "-Xlint:deprecation"),
    javaOptions in run ++= Seq("-Xms128m", "-Xmx1048m", "-Djava.library.path=./target/native"),
    libraryDependencies ++= Seq(

      "com.squareup.okhttp3" % "okhttp" % "3.12.1",
      "io.circe" %% "circe-core" % "0.10.0",
      "io.circe" %% "circe-generic" % "0.10.0",
      "io.circe" %% "circe-parser" % "0.10.0",

      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,

      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",

      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-remote" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-metrics" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
      "com.typesafe.akka" %% "akka-multi-node-testkit" % akkaVersion,

      "com.lihaoyi" %% "ujson" % "0.7.1",
      "com.lihaoyi" %% "ammonite-ops" % "1.4.4",

      "com.lightbend.akka" %% "akka-stream-alpakka-file" % "1.0-M1",
     	"org.typelevel" %% "cats-core" % "1.5.0",

      "fr.janalyse" %% "janalyse-ssh" % "0.10.3",
       
     	"com.squareup.okhttp3" % "okhttp" % "3.12.1",
	    "io.circe" %% "circe-core" % "0.10.0",
	    "io.circe" %% "circe-parser" % "0.10.0",
	    "io.circe" %% "circe-generic" % "0.10.0",

      "org.scalatest" %% "scalatest" % "3.0.5" % Test,
      "org.scalactic" %% "scalactic" % "3.0.5" % Test,
	    "org.scalacheck" %% "scalacheck" % "1.14.0" % Test,

      "io.kamon" % "sigar-loader" % "1.6.6-rev002"
    ),

    fork in run := true,
    parallelExecution in Test := false,
    mainClass in (Compile, run) := Some("com.wcrbrm.blockparser.Runner")
  ).configs(MultiJvm)
