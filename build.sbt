Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val scala3 = "3.7.3"

ThisBuild / scalaVersion := scala3
ThisBuild / crossScalaVersions := Seq(scala3)
ThisBuild / semanticdbEnabled := true

ThisBuild / sonatypeCredentialHost := xerial.sbt.Sonatype.sonatypeCentralHost

ThisBuild / organization := "io.github.ramytanios"
ThisBuild / organizationName := "ramytanios"
ThisBuild / homepage := Some(url("https://github.com/ramytanios/json-schema-lib"))
ThisBuild / licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0"))
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
  val circe      = "0.14.15"
  val munit      = "1.2.1"
  val http4s     = "0.23.30"
  val catsEffect = "3.5.4"
}

lazy val root =
  (project in file("."))
    .aggregate(libJVM, libJS, excel)
    .settings(publish / skip := true)

lazy val lib = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("lib"))
  .settings(
    name := "json-schema-lib",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core" % V.circe,
      "io.circe" %% "circe-generic" % V.circe,
      "io.circe" %% "circe-parser" % V.circe,
      "org.scalameta" %% "munit" % V.munit % Test
    ),
    scalacOptions -= "-Xfatal-warnings"
  )
  .jsSettings(
    scalaJSUseMainModuleInitializer := false,
    Test / scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) }
  )

lazy val libJVM = lib.jvm
lazy val libJS = lib.js

lazy val excel =
  (project in file("excel"))
    .dependsOn(libJVM)
    .settings(
      name := "json-schema-lib-excel",
      publish / skip := true,
      libraryDependencies ++= Seq(
        "io.circe"      %% "circe-core"          % V.circe,
        "org.http4s"    %% "http4s-ember-server"  % V.http4s,
        "org.http4s"    %% "http4s-dsl"           % V.http4s,
        "org.http4s"    %% "http4s-circe"          % V.http4s,
        "org.typelevel" %% "cats-effect"          % V.catsEffect,
        "org.scalameta" %% "munit"                % V.munit % Test
      ),
      scalacOptions -= "-Xfatal-warnings"
    )
