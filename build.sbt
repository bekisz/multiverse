name := "multiverse"
version := "0.1-SNAPSHOT"
scalaVersion := "2.11.12"
scalacOptions += "-target:jvm-1.8"

lazy val flinkVersion = "1.12.2"
lazy val log4jVersion = "2.12.1"
credentials += Credentials(Path.userHome / ".sbt" / "sonatype_credentials")

ThisBuild / organization := "com.github.bekisz"
ThisBuild / organizationName := "example"
ThisBuild / organizationHomepage := Some(url("http://example.com/"))

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/bekisz/multiverse"),
    "scm:git@github.com:bekisz/multiverse.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id    = "szabi",
    name  = "Szabolcs Beki",
    email = "szabi@apache.org",
    url   = url("https://github.com/bekisz")
  )
)

ThisBuild / description := "Artificial Life Framework"
ThisBuild / licenses := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))
ThisBuild / homepage := Some(url("https://github.com/example/project"))

// Remove all additional repository other than Maven Central from POM
ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
ThisBuild / publishMavenStyle := true


libraryDependencies ++= Seq(
  "org.apache.flink" %% "flink-streaming-scala" % flinkVersion,
   "org.apache.flink" %% "flink-table-planner-blink" % flinkVersion,
  //"org.apache.flink" %% "flink-table-planner" % flinkVersion,
  "org.apache.flink" %% "flink-clients" % flinkVersion,
  //"org.apache.flink" % "flink-csv" % flinkVersion,
  "org.apache.flink" %% "flink-connector-jdbc" % flinkVersion,
  "org.apache.logging.log4j" % "log4j-api" % log4jVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3",
   //"org.scalatest" %% "scalatest-funsuite" % "3.2.3" % Test,
  //"org.scalatest" %% "scalatest" % "3.2.3" % Test,

  "org.postgresql" % "postgresql" % "42.2.18.jre7"
).map(_.exclude("org.slf4j", "slf4j-log4j12"))
  .map(_.exclude("org.apache.calcite.avatica", "avatica-core"))



assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard

  case PathList("javax", "servlet", xs @ _*)         => MergeStrategy.first
  case PathList("javax", "transaction", xs @ _*)     => MergeStrategy.first
  case PathList("javax", "inject", xs @ _*)     => MergeStrategy.first
  case PathList("javax", "activation", xs @ _*)     => MergeStrategy.first
  case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first

  case x => MergeStrategy.first
}
