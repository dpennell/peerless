package peerless.lib.zquery

import zio.ZIO
import zio.logging.{Logging, log}
import zio.query.{CompletedRequestMap, Request}

object NeoDataSource {

  implicit class DataSourceImplSyntax[R <: Logging, E](f: ZIO[R, E, CompletedRequestMap]) {
    def recordFailures[A](description: String, requests: Iterable[Request[E, A]]): ZIO[R, Nothing, CompletedRequestMap] =
      f.catchAll { error =>
        log.error(s"$description failed with $error") *>
          ZIO.succeed {
            requests.foldLeft(CompletedRequestMap.empty) { case (resultMap, req) =>
              resultMap.insert(req)(Left(error))
            }
          }
      }
  }
}