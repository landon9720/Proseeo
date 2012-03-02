import AssemblyKeys._

name := "com.landonkuhn.proseeo.main.Proseeo"

version := "0.1"

scalaVersion := "2.9.1"

seq(assemblySettings: _*)

libraryDependencies += "commons-lang" % "commons-lang" % "2.5"

libraryDependencies += "commons-io" % "commons-io" % "1.4"
