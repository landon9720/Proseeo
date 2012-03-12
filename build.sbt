import AssemblyKeys._

name := "com.landonkuhn.proseeo.main.Proseeo"

version := "0.1"

scalaVersion := "2.9.1"

seq(assemblySettings: _*)

libraryDependencies += "com.novocode" % "junit-interface" % "0.8" % "test"

libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.1"

libraryDependencies += "commons-io" % "commons-io" % "2.1"

libraryDependencies += "org.joda" % "joda-convert" % "1.2"

libraryDependencies += "joda-time" % "joda-time" % "2.1"

libraryDependencies += "org.apache.lucene" % "lucene-core" % "3.5.0"

jarName in assembly := "proseeo.jar"
