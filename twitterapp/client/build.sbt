name := "client"

version := "1.0"

scalaVersion := "2.10.4"

resolvers += "Typesafe Repositiory" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.2"

libraryDependencies += "com.typesafe.akka" %% "akka-remote" % "2.3.2"
