val scala3 = "3.3.3"

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")

lazy val root = (project in file("."))
  .settings(
    name                                                  := "zio-agones",
    organization                                          := "com.devsisters",
    homepage                                              := Some(url("https://github.com/devsisters/zio-agones")),
    scalaVersion                                          := scala3,
    licenses                                              := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    scmInfo                                               := Some(
      ScmInfo(
        url("https://github.com/devsisters/zio-agones"),
        "scm:git:git@github.com:devsisters/zio-agones.git"
      )
    ),
    developers                                            := List(
      Developer("ghostdogpr", "Pierre Ricadat", "ghostdogpr@gmail.com", url("https://github.com/ghostdogpr"))
    ),
    libraryDependencies += "dev.zio"                      %% "zio-streams" % "2.1.25",
    Compile / PB.targets                                  := Seq(
      scalapb.gen(grpc = true) -> (Compile / sourceManaged).value / "scalapb"
    ),
    sbtprotoc.ProtocPlugin.autoImport.PB.protocExecutable := file(
      sys.env.getOrElse("PROTOC", "/opt/homebrew/bin/protoc")
    ),
    libraryDependencies ++= Seq(
      "io.grpc"               % "grpc-netty"           % "1.63.0",
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
      "dev.zio"              %% "zio"                  % "2.1.25"
    )
  )
