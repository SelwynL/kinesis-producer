import sbt._

object Dependencies {
  val resolutionRepos = Seq(
    "Sonatype OSS Releases"   at "http://oss.sonatype.org/content/repositories/releases/",
    "Typesafe"                at "http://repo.typesafe.com/typesafe/releases/",
    "Artima Maven Repository" at "http://repo.artima.com/releases"
  )

  object V {
    val avro          = "1.8.2"
    val kinesisClient = "1.9.1"
    val kafka         = "1.1.1"
    val enumeratum    = "1.5.13"
    val jackson       = "2.6.7"
    val circe         = "0.9.3"
    val cla           = "0.5.0"

    // Test
    val scalatest            = "3.0.5"
    val javatest             = "4.12"
  }

  val Libraries = Seq(
    "com.amazonaws"               %  "amazon-kinesis-client"  % V.kinesisClient,
    "com.concurrentthought.cla"   %% "command-line-arguments" % V.cla,
    "com.fasterxml.jackson.core"  %  "jackson-annotations"    % V.jackson,
    "org.apache.avro"             %  "avro"                   % V.avro,
    "org.apache.kafka"            %% "kafka"                  % V.kafka,
    "com.beachape"                %% "enumeratum"             % V.enumeratum,
    "io.circe"                    %% "circe-core"             % V.circe,
    "io.circe"                    %% "circe-generic"          % V.circe,
    "io.circe"                    %% "circe-parser"           % V.circe,

    // Test
    "org.scalatest"              %% "scalatest"              % V.scalatest   % "test",
    "junit"                      %  "junit"                  % V.javatest    % "test"
  )
}
