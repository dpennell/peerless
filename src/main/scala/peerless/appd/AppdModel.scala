package peerless.appd

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

object AppdModel {

  /** Common Values For ALl Topology Nodes
   * All AppDynamics entities available through the REST interface have
   * these two fields in common.
   */
  sealed trait Model {
    val id: Long
    val name: String
  }

  case class Application(id: Long, name: String, description: String = "") extends Model

  object Application {
    implicit val encoder: JsonEncoder[Application] = DeriveJsonEncoder.gen
    implicit val decoder: JsonDecoder[Application] = DeriveJsonDecoder.gen
  }

  object Model {
    implicit val encoder: JsonEncoder[Model] = DeriveJsonEncoder.gen
    implicit val decoder: JsonDecoder[Model] = DeriveJsonDecoder.gen
  }
}
