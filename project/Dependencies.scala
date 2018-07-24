import sbt._

object Dependencies {
  val resolutionRepos = Seq(
    "Sonatype OSS Releases"   at "http://oss.sonatype.org/content/repositories/releases/",
    "Typesafe"                at "http://repo.typesafe.com/typesafe/releases/",
    "Artima Maven Repository" at "http://repo.artima.com/releases"
  )

  object V {
    val avro                 = "1.8.2"
    val kinesisClient        = "1.9.1"
    val kafka                = "1.1.1"
    val enumeratum           = "1.5.13"
    val jackson              = "2.6.7"

    // Test
    val scalatest            = "3.0.5"
  }

  val Libraries = Seq(
    "com.amazonaws"              %  "amazon-kinesis-client"  % V.kinesisClient,
    "com.fasterxml.jackson.core" %  "jackson-annotations"    % V.jackson,
    "org.apache.avro"            %  "avro"                   % V.avro,
    "org.apache.kafka"           %% "kafka"                  % V.kafka,
    "com.beachape"               %% "enumeratum"             % V.enumeratum,

    // Test
    "org.scalatest"              %% "scalatest"              % V.scalatest   % "test"
  )
}
