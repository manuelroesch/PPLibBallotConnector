name := "PPLibBallotConnector"

version := "1.0"

scalaVersion := "2.11.6"

libraryDependencies += "mysql" % "mysql-connector-java" % "5.1.34"

libraryDependencies += "pdeboer" % "pplib_2.11" % "0.1-SNAPSHOT"

libraryDependencies ++= Seq(
  "org.scalikejdbc" %% "scalikejdbc"       % "2.2.2",
  "com.h2database"  %  "h2"                % "1.4.184",
  "org.scalikejdbc" %% "scalikejdbc-config"  % "2.2.2"
)

transitiveClassifiers in Global := Seq(Artifact.SourceClassifier)