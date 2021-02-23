package peerless.appd

import peerless.lib.sttp.Sttp.noCacheRequest
import sttp.client3.UriContext
import sttp.client3.asynchttpclient.zio._
import sttp.client3.zioJson._
import sttp.model.Uri
import zio.logging.Logging
import zio.query.{CompletedRequestMap, DataSource, ZQuery, Request => ZQRequest}
import zio.{Chunk, ZIO}

object AppdQuery {

  val userName: String                 = "admin"
  val account: String                  = "J9TechnologiesNFR"
  val password: String                 = "J2ee911pw"
  val userAndAccount: String           = s"$userName@$account"
  final val applicationsPrefix: String = "/controller/rest/applications"
  final val dbApplicationsPath: String = "/controller/rest/applications/Database Monitoring"
//  final val serverAppPath: String        = s"/controller/rest/applications/${AppDynamicsConfig.serverMonitoringAppName}"
  final val rollup: (String, String)    = "rollup"          -> "true"
  final val beforeNow: (String, String) = "time-range-type" -> "BEFORE_NOW"

  final val baseUri: Uri         = uri"https://J9TechnologiesNFR.saas.appdynamics.com"
  final val restUri: Uri         = baseUri.withPath("controller", "rest").withParam("output", "JSON")
  final val applicationsUri: Uri = restUri.addPath("applications")

  final val authRequest = noCacheRequest.auth.basic(userAndAccount, password)

  sealed trait AppdRequest[+A]             extends ZQRequest[Throwable, A]
  case object GetAllApplications           extends AppdRequest[List[AppdModel.Application]]
  case class GetApplication(appId: Long) extends AppdRequest[Option[AppdModel.Application]]

  lazy val AppdDataSource = new DataSource.Batched[Logging with SttpClient, AppdRequest[Any]] {

    override def run(requests: Chunk[AppdRequest[Any]]): ZIO[Logging with SttpClient, Nothing, CompletedRequestMap] = {
      val resultMap = CompletedRequestMap.empty
      requests.toList match {
        case request :: Nil =>
          request match {
            case r: GetAllApplications.type =>
              val req    = authRequest.get(applicationsUri).response(asJson[List[AppdModel.Application]])
              val result = send(req)
              result.fold(_ => resultMap, rsp => resultMap.insert(r)(rsp.body))
            case r @ GetApplication(appId) =>
              val req    = authRequest.get(applicationsUri.addPath(appId.toString)).response(asJson[List[AppdModel.Application]])
              val result = send(req)
              result.fold(_ => resultMap, rsp => resultMap.insert(r)(rsp.body.map(_.headOption))) // api returns a list
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

  def getAllApplications: ZQuery[Logging with SttpClient, Throwable, List[AppdModel.Application]] =
    ZQuery.fromRequest(GetAllApplications)(AppdDataSource)

  def getApplication(id: Long): ZQuery[Logging with SttpClient, Throwable, Option[AppdModel.Application]] =
    ZQuery.fromRequest(GetApplication(id))(AppdDataSource).fold(_ => None, identity)
}
