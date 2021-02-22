package peerless.appd

import peerless.appd.AppdModel.Application
import zio.json.{DecoderOps, EncoderOps}
import zio.test.Assertion._
import zio.test._

object AppdModelSpec extends DefaultRunnableSpec {

  def spec =
    suite("AppdModelSpec")(
      test("encode Application") {
        val app      = Application(42L, "bubba")
        val appJs    = app.toJson
        val expected = """{"id":42,"name":"bubba","description":""}"""
        assert(appJs)(equalTo(expected))
      },
      test("decode Application with default field") {
        val app      = """{"id":42,"name":"bubba"}""".fromJson[Application]
        val expected = Right(Application(42L, "bubba", ""))
        assert(app)(equalTo(expected))
      }
    )
}
