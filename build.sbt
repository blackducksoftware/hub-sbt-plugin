sbtPlugin := true

name := "sbt-hub-plugin"

organization := "com.blackducksoftware"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.10.6"

resolvers += "Maven Repository" at "https://repo.maven.apache.org/maven2/"
resolvers += "JCenter Repository" at "https://jcenter.bintray.com"
resolvers += Resolver.mavenLocal

libraryDependencies ++= Seq(
  "com.blackducksoftware.integration" % "hub-common" % "7.4.0-SNAPSHOT",
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
  "junit" % "junit" % "4.11" % "test"
)

licenses += ("Apache License 2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))

publishMavenStyle := true

publishArtifact in Test := false

pomExtra := (
  <scm>
    <connection>scm:git:git://github.com/blackducksoftware/sbt-hub-plugin.git/</connection>
    <developerConnection>scm:git:git@github.com:blackducksoftware/sbt-hub-plugin.git</developerConnection>
    <url>https://github.com/blackducksoftware/sbt-hub-plugin</url>
  </scm>