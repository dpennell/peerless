package peerless.files

import zio._
import zio.blocking.Blocking
import zio.console.{putStrLn, Console}
import zio.stream._

import java.nio.file.Paths

object WriteToFile extends App {

  def writeToFile = {

    val sink: ZSink[Blocking, Throwable, Byte, Byte, Long] = ZSink
      .fromFile(Paths.get("data/out.txt"))

    val lines = Seq(
      "hello world",
      "goodbye"
    )

    val stream: ZIO[Blocking with Console, Throwable, Long] = ZStream
      .fromIterable(lines)
      .tap(str => putStrLn(str))
      .mapConcat(s => ("WriteToFile: " + s + '\n').map(_.toByte))
      .run(sink)

    stream
  }

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    writeToFile.exitCode
}
