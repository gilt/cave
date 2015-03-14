import com.typesafe.sbt.SbtNativePackager._
import NativePackagerKeys._
import play.PlayImport._

name := "cavellc"

scalaVersion := "2.10.4"

lazy val akkaVersion = "2.3.4"

lazy val commonSettings: Seq[Def.Setting[_]] = Seq(
  organization := "com.cavellc",
  name <<= name("cave-" + _),
  version := "git describe --tags --dirty --always".!!.stripPrefix("v").trim,
  resolvers += Resolver.sonatypeRepo("snapshots"),
  resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
  libraryDependencies ++= Seq(
    "io.dropwizard.metrics" % "metrics-core" % "3.1.0",
    "io.dropwizard.metrics" % "metrics-jvm" % "3.1.0",
    "org.scalatest" %% "scalatest" % "2.1.2" % "test"
  )
)

lazy val commonDockerSettings: Seq[Def.Setting[_]] = Seq(
  maintainer in Docker := "TWAIN <twain@gilt.com>",
  dockerBaseImage := "cavellc/ubuntu-openjdk-7-jre-headless:12.04",
  dockerRepository := Some("cavellc"),
  defaultLinuxInstallLocation in Docker := "/cavellc"
)

lazy val bashScriptSettings: Seq[Def.Setting[_]] = Seq(
  NativePackagerKeys.bashScriptExtraDefines ++= Seq(
    """addJava "-Djava.net.preferIPv4Stack=true" """,
    """addJava "-Dnetworkaddress.cache.ttl=5" """,
    """addJava "-Dnetworkaddress.cache.negative.ttl=0" """,
    """addJava "-Dconfig.resource=application.conf" """,
    """addJava "-Dlogger.file=/cavellc/conf/logger.xml" """
  )
)

lazy val commonScoverageSettings: Seq[Def.Setting[_]] = Seq(
  ScoverageKeys.excludedPackages in ScoverageCompile := "<empty>;routes_.*",
  ScoverageKeys.highlighting := true
)

lazy val core = project
  .settings(commonSettings: _*)
  .settings(instrumentSettings: _*)
  .settings(commonScoverageSettings: _*)
  .settings(parallelExecution in ScoverageTest := false: _*)
  .settings(parallelExecution in Test := false: _*)
  .settings(
    libraryDependencies ++= Seq(
      "com.amazonaws" % "aws-java-sdk" % "1.9.13",
      "com.amazonaws" % "amazon-kinesis-client" % "1.0.0",
      "org.apache.commons" % "commons-lang3" % "3.3.2",
      "com.ning" % "async-http-client" % "1.8.9",
      "com.typesafe.play" %% "play-json" % "2.2.2",
      "com.typesafe.slick" %% "slick" % "2.1.0-M2",
      "org.postgresql" % "postgresql" % "9.3-1101-jdbc4",
      "com.zaxxer" % "HikariCP-java6" % "2.1.0",
      "org.mindrot" % "jbcrypt" % "0.3m",
      "org.mockito" % "mockito-all" % "1.9.5" % "test",
      "com.h2database" % "h2" % "1.3.175" % "test",
      "ch.qos.logback" % "logback-classic" % "1.1.1"
    )
  )

lazy val logs = project
  .settings(commonSettings: _*)
  .settings(instrumentSettings: _*)
  .settings(commonScoverageSettings: _*)
  .settings(parallelExecution in ScoverageTest := false: _*)
  .settings(parallelExecution in Test := false: _*)
  .settings(
    libraryDependencies ++= Seq(
      "com.amazonaws" % "aws-java-sdk" % "1.9.13",
      "ch.qos.logback" % "logback-classic" % "1.1.1"
    )
  )

lazy val api = project
  .dependsOn(core % "test->test;compile->compile")
  .dependsOn(logs % "test->test;compile->compile")
  .enablePlugins(PlayScala)
  .settings(commonSettings: _*)
  .settings(bashScriptSettings: _*)
  .settings(instrumentSettings: _*)
  .settings(commonScoverageSettings: _*)
  .settings(parallelExecution in ScoverageTest := false: _*)
  .settings(parallelExecution in Test := false: _*)
  .settings(commonDockerSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.play.plugins" %% "play-plugins-mailer" % "2.3.0"
    ),
    dockerExposedPorts in Docker := Seq(7001)
  )

lazy val scheduler = project
  .dependsOn(core % "test->test;compile->compile")
  .dependsOn(logs % "test->test;compile->compile")
  .enablePlugins(PlayScala)
  .settings(commonSettings: _*)
  .settings(bashScriptSettings: _*)
  .settings(instrumentSettings: _*)
  .settings(commonScoverageSettings: _*)
  .settings(commonDockerSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"
    ),
    dockerExposedPorts in Docker := Seq(9000)
  )

lazy val worker = project
  .dependsOn(core % "test->test;compile->compile")
  .dependsOn(logs % "test->test;compile->compile")
  .enablePlugins(PlayScala)
  .settings(commonSettings: _*)
  .settings(bashScriptSettings: _*)
  .settings(instrumentSettings: _*)
  .settings(commonScoverageSettings: _*)
  .settings(commonDockerSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"
    ),
    dockerExposedPorts in Docker := Seq(7004)
  )

lazy val www = project
  .dependsOn(logs % "test->test;compile->compile")
  .enablePlugins(PlayScala)
  .settings(PlayKeys.routesImport += "com.gilt.cavellc.models.Bindables._")
  .settings(commonSettings: _*)
  .settings(commonDockerSettings: _*)
  .settings(Seq(NativePackagerKeys.bashScriptExtraDefines ++= Seq(
    """addJava "-Djava.net.preferIPv4Stack=true" """,
    """addJava "-Dnetworkaddress.cache.ttl=5" """,
    """addJava "-Dnetworkaddress.cache.negative.ttl=0" """
  )): _*)
  .settings(
    libraryDependencies ++= Seq(ws, "com.typesafe.play" %% "play-json" % "2.2.2"),
    TwirlKeys.templateImports += "com.gilt.cavellc.models._",
    dockerExposedPorts in Docker := Seq(7002)
  )

lazy val dbwriter = project
  .dependsOn(core % "test->test;compile->compile")
  .dependsOn(logs % "test->test;compile->compile")
  .enablePlugins(PlayScala)
  .settings(commonSettings: _*)
  .settings(bashScriptSettings: _*)
  .settings(commonDockerSettings: _*)
  .settings(
    dockerExposedPorts in Docker := Seq(9000)
  )
