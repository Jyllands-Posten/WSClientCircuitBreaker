name := """WSClientCircuitBreaker"""
organization := "dk.jp"
scalaVersion := "2.12.7"
ensimeScalaVersion in ThisBuild := "2.12.7"
scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "UTF-8", "-Xfatal-warnings", "-Xlint:missing-interpolator", "-Ywarn-unused", "-Ywarn-dead-code", "-Ywarn-numeric-widen", "-language:implicitConversions")

libraryDependencies ++= Seq("com.typesafe.play" %% "play-ws" % "2.6.17")

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"
