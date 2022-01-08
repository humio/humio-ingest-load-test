enablePlugins(GatlingPlugin)
enablePlugins(GraalVMNativeImagePlugin)

scalaVersion := "2.12.12"

scalacOptions := Seq(
  "-encoding", "UTF-8", "-deprecation",
  "-feature", "-unchecked", "-language:implicitConversions", "-language:postfixOps")

libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % "3.0.2"
libraryDependencies += "io.gatling"            % "gatling-test-framework"    % "3.0.2"
libraryDependencies += "org.apache.commons"    % "commons-math3"             % "3.6.1"
libraryDependencies += "org.scalatra.scalate" %% "scalate-core" % "1.9.5"
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.9.2"
libraryDependencies += "com.github.tototoshi" %% "scala-csv" % "1.3.6"

mainClass in Compile := Some("io.gatling.app.Gatling")

assemblyJarName in assembly := "perftest.jar"

assemblyMergeStrategy in assembly := {
    case PathList("META-INF", xs @ _*) => MergeStrategy.discard
    case p if p.endsWith("gatling-version.properties") =>
      MergeStrategy.first
    case rest =>
      MergeStrategy.first
      //val oldStrategy = (assemblyMergeStrategy in assembly).value
      //oldStrategy(rest)
}