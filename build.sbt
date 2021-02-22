val ZIO = "1.0.4-2"

val zioConfigVersion = "1.0.0"

val sttpVersion = "3.1.3"

lazy val root = (project in file("."))
  .settings(
    name := "peerless",
    organization := "com.streamweaver",
    scalaVersion := "2.13.4",
    scalacOptions := Seq(
      "-deprecation", "-encoding", "UTF-8", "-feature", "-unchecked", "-Ywarn-unused:params,-implicits",
      "-language:higherKinds", "-language:existentials", "-explaintypes", "-Yrangepos",
      "-Xlint:_,-missing-interpolator,-type-parameter-shadow", "-Ywarn-numeric-widen", "-Ywarn-value-discard",
      "-Xfatal-warnings"
    ),
    scalacOptions in Compile in console := Seq(),
    initialCommands in Compile in console :=
      """|import zio._
         |import zio.console._
         |import zio.duration._
         |import zio.stream._
         |import zio.Runtime.default._
         |implicit class RunSyntax[A](io: ZIO[ZEnv, Any, A]){ def unsafeRun: A = Runtime.default.unsafeRun(io.provideLayer(ZEnv.live)) }
      """.stripMargin,
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
    libraryDependencies ++= Seq(
      "dev.zio"                       %% "zio"                           % ZIO,
      "dev.zio"                       %% "zio-streams"                   % ZIO,
      "dev.zio"                       %% "zio-test"                      % ZIO % "test",
      "dev.zio"                       %% "zio-test-sbt"                  % ZIO % "test",
      "dev.zio"                       %% "zio-config"                    % zioConfigVersion,
      "dev.zio"                       %% "zio-config-magnolia"           % zioConfigVersion,
      "dev.zio"                       %% "zio-config-refined"            % zioConfigVersion,
      "dev.zio"                       %% "zio-config-typesafe"           % zioConfigVersion,
      "dev.zio"                       %% "zio-json"                      % "0.0.1",
      "dev.zio"                       %% "zio-logging-slf4j"             % "0.5.6",
      "dev.zio"                       %% "zio-query"                     % "0.2.6",
      "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % sttpVersion,
      "com.softwaremill.sttp.client3" %% "json-common"                   % sttpVersion,
    ),
    scalafmtOnCompile := true
  )
