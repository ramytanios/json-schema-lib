Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val scala3 = "3.7.3"

ThisBuild / scalaVersion       := scala3
ThisBuild / crossScalaVersions := Seq(scala3)
ThisBuild / semanticdbEnabled  := true

ThisBuild / organization     := "io.github.ramytanios"
ThisBuild / organizationName := "ramytanios"
ThisBuild / homepage         := Some(url("https://github.com/ramytanios/json-schema-lib"))
ThisBuild / licenses         := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / developers := List(
  Developer("ramytanios", "Ramy Tanios", "", url("https://github.com/ramytanios"))
)
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/ramytanios/json-schema-lib"),
    "scm:git:git@github.com:ramytanios/json-schema-lib.git"
  )
)

lazy val V = new {
  val circe = "0.14.15"
  val cats = "2.13.0"
  val munit = "1.2.1"
}

lazy val root =
  (project in file(".")).aggregate(lib).settings(publish / skip := true)

lazy val lib = project.in(file("lib")).settings(
  name := "json-schema-lib",
  libraryDependencies ++= Seq(
    "io.circe" %% "circe-core" % V.circe,
    "io.circe" %% "circe-generic" % V.circe,
    "io.circe" %% "circe-parser" % V.circe,
    "org.scalameta" %% "munit" % V.munit % Test
  ),
  scalacOptions -= "-Xfatal-warnings"
)
