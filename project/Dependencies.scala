import sbt._

object Dependencies {
  val resolutionRepos = Seq(
    "Sonatype OSS Releases"   at "http://oss.sonatype.org/content/repositories/releases/",
    "Typesafe"                at "http://repo.typesafe.com/typesafe/releases/",
    "Artima Maven Repository" at "http://repo.artima.com/releases"
  )
  object V {
    // Scala
    val avro                 = "1.8.2"
    val kinesisClient        = "1.9.1"
    // Scala (test only)
    val scalatest            = "3.0.5"
  }
  val Libraries = Seq(
    // Scala
    "com.amazonaws"              %  "amazon-kinesis-client"  % V.kinesisClient,
    "com.fasterxml.jackson.core" %  "jackson-annotations"    % "2.6.7",
    "org.apache.avro"            %  "avro"                   % V.avro,
    // Scala (test only)
    "org.scalatest"              %% "scalatest"              % V.scalatest   % "test"
  )
}
