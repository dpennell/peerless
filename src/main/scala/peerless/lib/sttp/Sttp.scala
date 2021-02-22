package peerless.lib.sttp

import sttp.client3.{basicRequest, Empty, RequestT}
import sttp.model.Header
import sttp.model.headers.CacheDirective.NoCache

object Sttp {

  val noCacheRequest: RequestT[Empty, Either[String, String], Any] = basicRequest.headers(Header.cacheControl(NoCache))
}
