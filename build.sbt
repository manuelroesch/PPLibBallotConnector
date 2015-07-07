name := "PPLibBallotConnector"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.34"

libraryDependencies += "pdeboer" % "pplib_2.11" % "0.1-SNAPSHOT"

libraryDependencies += "org.slf4j" % "slf4j-log4j12" % "1.7.5"

libraryDependencies += "junit" % "junit" % "4.12"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.0-SNAP4"

libraryDependencies ++= Seq(
  "org.scalikejdbc" %% "scalikejdbc"       % "2.2.7",
  "com.h2database"  %  "h2"                % "1.4.184",
  "org.scalikejdbc" %% "scalikejdbc-config"  % "2.2.7",
  "org.scalikejdbc" %% "scalikejdbc-play-initializer" % "2.4.0"
)

resolvers += Resolver.file("Local repo", file(System.getProperty("user.home") + "/.ivy2/local"))(Resolver.ivyStylePatterns)

ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

transitiveClassifiers in Global := Seq(Artifact.SourceClassifier)

assemblyMergeStrategy in assembly := {
  case "log4j.properties" => MergeStrategy.concat
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

