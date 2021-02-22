package peerless.appd

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

object AppdModel {

  /** Common Values For ALl Topology Nodes
   * All AppDynamics entities available through the REST interface have
   * these two fields in common.
   */
  trait AppdModel {
    val id: Long
    val name: String
  }

  case class Application(id: Long, name: String, description: String = "") extends AppdModel

  object Application {
    implicit val encoder: JsonEncoder[Application] = DeriveJsonEncoder.gen[Application]
    implicit val decoder: JsonDecoder[Application] = DeriveJsonDecoder.gen[Application]
  }

}
