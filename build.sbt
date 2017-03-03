sbtPlugin := true

name := "sbt-hub-plugin"

organization := "com.blackducksoftware.integration"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.10.6"

resolvers += Resolver.mavenLocal
resolvers += "Maven Repository" at "https://repo.maven.apache.org/maven2/"
resolvers += "JCenter Repository" at "https://jcenter.bintray.com"
resolvers += "Sonatype Releases Repository" at "https://oss.sonatype.org/content/repositories/releases"
resolvers += "Sonatype Snapshots Repository" at "https://oss.sonatype.org/content/repositories/snapshots"

libraryDependencies ++= Seq(
  "com.blackducksoftware.integration" % "hub-common" % "8.0.0-SNAPSHOT",
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
  "junit" % "junit" % "4.11" % "test"
)

resourceGenerators in Compile <+= Def.task {
    val file = (resourceManaged in Compile).value / "version.txt"
    val contents = version.value
    IO.write(file, contents)
    Seq(file)
}

licenses += ("Apache License 2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))

publishMavenStyle := true

publishArtifact in Test := false

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots") 
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomExtra := (
  <url>https://github.com/blackducksoftware/sbt-hub-plugin</url>
  <scm>
    <connection>scm:git:git://github.com/blackducksoftware/sbt-hub-plugin.git/</connection>
    <developerConnection>scm:git:git@github.com:blackducksoftware/sbt-hub-plugin.git</developerConnection>
    <url>https://github.com/blackducksoftware/sbt-hub-plugin</url>
  </scm>
  <developers>
    <developer>
      <id>blackduckoss</id>
      <name>Black Duck OSS</name>
      <email>bdsoss@blackducksoftware.com</email>
      <organization>Black Duck Software, Inc.</organization>
      <organizationUrl>http://www.blackducksoftware.com</organizationUrl>
      <roles>
        <role>developer</role>
      </roles>
      <timezone>America/New_York</timezone>
    </developer>
  </developers>
)