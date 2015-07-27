name := "PPLibBallotConnector"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.slf4j"                 %  "slf4j-log4j12"                % "1.7.5",
  "mysql"                     %  "mysql-connector-java"         % "5.1.34",
  "org.apache.httpcomponents" %  "httpclient"                   % "4.5",
  "com.typesafe"              %  "config"                       % "1.2.1",
  "junit"                     %  "junit"                        % "4.8.1" % "test",
  "org.scalikejdbc"           %% "scalikejdbc"                  % "2.2.7",
  "com.h2database"            %  "h2"                           % "1.4.184",
  "org.scalikejdbc"           %% "scalikejdbc-config"           % "2.2.7",
  "org.scalikejdbc"           %% "scalikejdbc-play-initializer" % "2.4.0"
)

libraryDependencies += "pdeboer" %% "pplib" % "0.1-SNAPSHOT"

resolvers += Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)

ivyScala := ivyScala.value map {
  _.copy(overrideScalaVersion = true)
}

transitiveClassifiers in Global := Seq(Artifact.SourceClassifier)

assemblyMergeStrategy in assembly := {
  case "log4j.properties" => MergeStrategy.concat
  case "application.conf" => MergeStrategy.concat
  case "application.conf_default" => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

mainClass in assembly := Some("ch.uzh.ifi.pdeboer.pplib.hcomp.ballot.integrationtest.console.ConsoleIntegrationTest")

// To Skip Tests:
//test in assembly := {}

assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = true)