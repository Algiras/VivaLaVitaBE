import sbt.Keys._
import sbt._

val Http4sVersion = "0.20.0-M6"
val Specs2Version = "4.1.0"
val LogbackVersion = "1.2.3"
val CirceVersion = "0.9.3"
val tsecVersion = "0.1.0-M3"
val shapelessVersion = "2.3.3"
val tsecV = "0.1.0-M3"
val pureConfigVersion = "0.10.2"

resolvers += Resolver.sonatypeRepo("snapshots")

lazy val root = (project in file("."))
  .settings(
    organization := "com.wix",
    name := "VivaLaVita",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.7",
    scalacOptions := Seq("-unchecked", "-deprecation", "-Ypartial-unification", "-Yrangepos","-language:higherKinds",  "-language:postfixOps"),
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "1.4.0",

      "com.typesafe.slick" %% "slick" % "3.2.1",
      "com.typesafe.slick" %% "slick-hikaricp" % "3.2.1",

      "mysql" % "mysql-connector-java" % "8.0.14",
      "com.h2database" % "h2" % "1.4.196",

      "com.github.nscala-time" %% "nscala-time" % "2.22.0",
      "io.github.jmcardon" %% "tsec-http4s" % tsecVersion,
      "com.chuusai" % "shapeless_2.12" % shapelessVersion,

      "com.github.pureconfig" %% "pureconfig"             % pureConfigVersion,
      "com.github.pureconfig" %% "pureconfig-cats-effect" % pureConfigVersion,
      "com.github.pureconfig" %% "pureconfig-generic"     % pureConfigVersion,

      "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,

      "io.circe" %% "circe-core" % CirceVersion,
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.circe" %% "circe-literal" % CirceVersion,

      "org.specs2" %% "specs2-core" % Specs2Version % "test",

      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
      "ch.qos.logback" % "logback-classic" % LogbackVersion,

      "com.github.pureconfig" %% "pureconfig-cats-effect" % pureConfigVersion,
      "com.github.pureconfig" %% "pureconfig-enumeratum"  % pureConfigVersion,
      "com.github.pureconfig" %% "pureconfig-generic"     % pureConfigVersion
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.6"),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.2.4")
  )


addCompilerPlugin(
  "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full
)

