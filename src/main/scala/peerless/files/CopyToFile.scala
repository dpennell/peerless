package peerless.files

import zio._
import zio.blocking.Blocking
import zio.console.putStrLn
import zio.stream._

import java.nio.file.Paths

object CopyToFile extends App {

  def copyFile = {

    val sink: ZSink[Blocking, Throwable, Byte, Byte, Long] = ZSink
      .fromFile(Paths.get("data/out.txt"))

    val stream = ZStream
      .fromFile(Paths.get("data/in.txt"))
      .transduce(ZTransducer.utf8Decode >>> ZTransducer.splitLines)
      .tap(str => putStrLn(str))
      .mapConcat(s => ("CopyToFile: " + s + '\n').map(_.toByte))
      .run(sink)

    stream
  }

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    copyFile.exitCode
}
