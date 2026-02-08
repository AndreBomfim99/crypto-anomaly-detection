name := "crypto-anomaly-detection"

version := "0.1.0"

scalaVersion := "2.12.19"

organization := "com.andrebomfim"

libraryDependencies ++= Seq(
  // Spark
  "org.apache.spark" %% "spark-core" % "3.5.0" % "provided",
  "org.apache.spark" %% "spark-sql" % "3.5.0" % "provided",
  "org.apache.spark" %% "spark-streaming" % "3.5.0" % "provided",
  
  // HTTP Client para CoinGecko API
  "com.softwaremill.sttp.client3" %% "core" % "3.9.5",
  "com.softwaremill.sttp.client3" %% "circe" % "3.9.5",
  
  // JSON parsing
  "io.circe" %% "circe-core" % "0.14.6",
  "io.circe" %% "circe-generic" % "0.14.6",
  "io.circe" %% "circe-parser" % "0.14.6",
  
  // AWS SDK para S3 e SNS (LocalStack)
  "software.amazon.awssdk" % "s3" % "2.25.11",
  "software.amazon.awssdk" % "sns" % "2.25.11",
  
  // Logging
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "ch.qos.logback" % "logback-classic" % "1.4.14",
  
  // Config
  "com.typesafe" % "config" % "1.4.3",
  
  // Testing
  "org.scalatest" %% "scalatest" % "3.2.18" % Test
)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-unchecked"
)
