package peerless.appd

import peerless.lib.sttp.Sttp.noCacheRequest
import sttp.client3.asynchttpclient.zio._
import sttp.client3.zioJson._
import sttp.client3.{Response, UriContext}
import sttp.model.headers.CacheDirective.NoCache
import sttp.model.{Header, StatusCode, Uri}
import zio.logging.Logging
import zio.query.{CompletedRequestMap, DataSource, Request => ZQRequest}
import zio.{Chunk, ZIO}

object AppdQuery {

  val baseUri: Uri                     = uri"http2://J9TechnologiesNFR.saas.appdynamics.com"
  val restUri: Uri                     = baseUri.withPath("controller", "rest").withParam("output", "JSON")
  val userName: String                 = ""
  val account: String                  = ""
  val password: String                 = ""
  val user: String                     = s"$userName@$account"
  final val applicationsPrefix: String = "/controller/rest/applications"
  final val dbApplicationsPath: String = "/controller/rest/applications/Database Monitoring"
//  final val serverAppPath: String        = s"/controller/rest/applications/${AppDynamicsConfig.serverMonitoringAppName}"
  final val rollup: (String, String)    = "rollup"          -> "true"
  final val beforeNow: (String, String) = "time-range-type" -> "BEFORE_NOW"

  final val applicationsUri = restUri.withPath("applications")

  sealed trait AppdRequest[+A]             extends ZQRequest[Throwable, A]
  case object GetAllApplications           extends AppdRequest[List[AppdModel.Application]]
  case class GetApplication(appId: String) extends AppdRequest[AppdModel.Application]

  lazy val AppdDataSource = new DataSource.Batched[Logging with SttpClient, AppdRequest[Any]] {

    override def run(requests: Chunk[AppdRequest[Any]]): ZIO[Logging with SttpClient, Nothing, CompletedRequestMap] = {
      val resultMap = CompletedRequestMap.empty
      requests.toList match {
        case request :: Nil =>
          request match {
            case r: GetAllApplications.type =>
              val req    = noCacheRequest.get(applicationsUri).response(asJson[List[AppdModel.Application]])
              val result = send(req)
              result.either.map(resultMap.insert(r))
            case r @ GetApplication(appId) =>
              val req    = noCacheRequest.get(applicationsUri.withPath(appId))
              val result = send(req)
              result.either.map(resultMap.insert(r))
          }

        case batch =>
          // see https://github.com/vigoo/aws-query/blob/master/src/main/scala/io/github/vigoo/awsquery/sources/ebquery.scala
          // for implementation strategy

          // nothing to do here so far

          // todo: handle batch of GetApplication(id) as above - see Aws example for framework assist

          // todo: consider cache life-cycle for zio.query and needs of SWP
          ZIO.succeed(resultMap)
      }
    }

    override val identifier: String = "AppdDataSource"
  }

  // todo: figure out how to configure connection pool, etc. for asynchHttp backend

  val request = noCacheRequest.get(uri"https://httpbin.org/get").headers(Header.cacheControl(NoCache))

  val sent: ZIO[SttpClient, Throwable, Response[Either[String, String]]] = send(request)

  val xxx: ZIO[SttpClient, Throwable, (StatusCode, Either[String, String])] = for {
    r <- sent
  } yield r.code -> r.body
}

/*
def run(requests: Chunk[GetUserName]): ZIO[Any, Nothing, CompletedRequestMap] = {
  val resultMap = CompletedRequestMap.empty
  requests.toList match {
    case request :: Nil =>
      // get user by ID e.g. SELECT name FROM users WHERE id = $id
      val result: Task[String] = ???
      result.either.map(resultMap.insert(request))
    case batch =>
      // get multiple users at once e.g. SELECT id, name FROM users WHERE id IN ($ids)
      val result: Task[List[(Int, String)]] = ???
      result.fold(
        err => requests.foldLeft(resultMap) { case (map, req) => map.insert(req)(Left(err)) },
        _.foldLeft(resultMap) { case (map, (id, name)) => map.insert(GetUserName(id))(Right(name)) }
      )
  }
}
 */
