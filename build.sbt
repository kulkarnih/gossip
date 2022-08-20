ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

Compile / mainClass := Some("org.membership.gossip.Gossip")

lazy val root = (project in file("."))
  .settings(
    name := "gossip"
  )
