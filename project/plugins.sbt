lazy val V = new {
  val tpolecat = "0.5.1"
  val updates = "0.6.3"
  val `scala-fix` = "0.14.0"
  val `scala-fmt` = "2.5.4"
  val `ci-release` = "1.9.3"
  val scalajs = "1.20.1"
  val `scalajs-crossproject` = "1.3.2"
}

addSbtPlugin("org.typelevel" % "sbt-tpolecat" % V.tpolecat)
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % V.updates)
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % V.`scala-fix`)
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % V.`scala-fmt`)
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % V.`ci-release`)
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % V.`scalajs-crossproject`)
addSbtPlugin("org.scala-js" % "sbt-scalajs" % V.scalajs)
