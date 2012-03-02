import AssemblyKeys._

name := "com.landonkuhn.proseeo.main.Proseeo"

version := "0.1"

scalaVersion := "2.9.1"

seq(assemblySettings: _*)

libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.1"

libraryDependencies += "commons-io" % "commons-io" % "2.1"

libraryDependencies += "org.joda" % "joda-convert" % "1.2"

libraryDependencies += "joda-time" % "joda-time" % "2.1"
