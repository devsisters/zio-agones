val scala213 = "2.13.14"
val scala3   = "3.3.3"
val allScala = Seq(scala213, scala3)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")

lazy val root = (project in file("."))
  .settings(
    name                 := "zio-agones",
    organization         := "com.devsisters",
    homepage             := Some(url("https://github.com/devsisters/zio-agones")),
    scalaVersion         := scala213,
    crossScalaVersions   := allScala,
    licenses             := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    scmInfo              := Some(
      ScmInfo(
        url("https://github.com/devsisters/zio-agones"),
        "scm:git:git@github.com:devsisters/zio-agones.git"
      )
    ),
    developers           := List(
      Developer("ghostdogpr", "Pierre Ricadat", "ghostdogpr@gmail.com", url("https://github.com/ghostdogpr"))
    ),
    Compile / PB.targets := Seq(
      scalapb.gen(grpc = true)          -> (Compile / sourceManaged).value / "scalapb",
      scalapb.zio_grpc.ZioCodeGenerator -> (Compile / sourceManaged).value / "scalapb"
    ),
    libraryDependencies ++= Seq(
      "io.grpc"               % "grpc-netty"           % "1.63.0",
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion
    )
  )
