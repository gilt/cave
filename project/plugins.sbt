// Comment to get more information during initialization
logLevel := Level.Warn


// The Typesafe repository
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.4")

// Newer sbt native packager
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "0.7.5")

resolvers += Classpaths.sbtPluginReleases

addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "0.99.5.1")

// Secure Social
resolvers += Resolver.sonatypeRepo("snapshots")