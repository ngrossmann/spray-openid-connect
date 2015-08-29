lazy val commonSettings = Seq(
  organization := "net.n12n.openid",
  licenses := List(("Apache License Version 2.0", url("http://www.apache.org/licenses/"))),
  version := "0.1.0-SNAPSHOT",
  scalaVersion  := "2.11.6",
  scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8", "-feature", "-language:postfixOps"),
  fork in Test := true,
  resolvers ++= Seq("spray repo" at "http://repo.spray.io/"),
  libraryDependencies ++= {
    val akkaV = "2.3.9"
    val sprayV = "1.3.3"
    Seq(
      "io.spray"            %%   "spray-caching"   % sprayV,
      "io.spray"            %%   "spray-can"     % sprayV,
      "io.spray"            %%   "spray-routing" % sprayV,
      "io.spray"            %%   "spray-client" % sprayV,
      "io.spray"            %%  "spray-json" % "1.3.1",
      "com.typesafe.akka"   %%  "akka-actor"    % akkaV,
      "com.typesafe.akka"   %%  "akka-testkit"  % akkaV,
      "com.typesafe.akka"   %%  "akka-slf4j"  % akkaV,
      "org.slf4j" % "slf4j-api" % "1.7.5",
      "org.slf4j" % "jcl-over-slf4j" % "1.7.5",
      "ch.qos.logback" % "logback-classic" % "1.0.13",
      "org.scalatest" %% "scalatest" % "2.2.4" % "test",
      "io.spray" %% "spray-testkit" % sprayV
    )
  }
)

lazy val connect = (project in file("connect")).settings(commonSettings: _*).
  settings(
    name := "openid-connect"
  )

lazy val connectTest = (project in file("connect-test")).dependsOn(connect).
  settings(commonSettings: _*).
  settings(Revolver.settings).
  settings(
    name := "openid-connect-test"
  )