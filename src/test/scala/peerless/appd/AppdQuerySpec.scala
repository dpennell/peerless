package peerless.appd

import sttp.client3.asynchttpclient.zio.{AsyncHttpClientZioBackend, SttpClient}
import zio.ZLayer
import zio.clock.Clock
import zio.console.Console
import zio.logging.Logging
import zio.test._
import zio.test.Assertion._
import zio.test.environment.TestEnvironment

object AppdQuerySpec extends DefaultRunnableSpec {

  val layer: ZLayer[Any with Console with Clock, Throwable, SttpClient with Logging] =
    AsyncHttpClientZioBackend.layer() ++ Logging.console()

  val existingApp      = AppdModel.Application(6006, "easyTravel-k8s")
  val nonExistentAppId = 42L

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("AppdQuerySpec")(
      testM("get all applications") {
        for {
          apps <- AppdQuery.getAllApplications.run
        } yield assert(apps)(contains(existingApp))
      },
      testM("get an existing application") {
        for {
          maybeApp <- AppdQuery.getApplication(existingApp.id).run
        } yield assert(maybeApp)(equalTo(Some(existingApp)))
      },
      testM("get a non-existent application") {
        for {
          maybeApp <- AppdQuery.getApplication(nonExistentAppId).run
        } yield assert(maybeApp)(equalTo(None))
      }
    ).provideCustomLayerShared(layer).mapError(TestFailure.fail(_))
}
