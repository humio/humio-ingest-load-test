enablePlugins(GatlingPlugin)
enablePlugins(GraalVMNativeImagePlugin)

scalaVersion := "2.12.8"

scalacOptions := Seq(
  "-encoding", "UTF-8", "-deprecation",
  "-feature", "-unchecked", "-language:implicitConversions", "-language:postfixOps")

libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % "3.0.2"
libraryDependencies += "io.gatling"            % "gatling-test-framework"    % "3.0.2"

mainClass in Compile := Some("io.gatling.app.Gatling")

assemblyJarName in assembly := "perftest.jar"

assemblyMergeStrategy in assembly := {
    case PathList("META-INF", xs @ _*) => MergeStrategy.discard
    case p if p.endsWith("gatling-version.properties") =>
      MergeStrategy.first
    case rest =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(rest)
}