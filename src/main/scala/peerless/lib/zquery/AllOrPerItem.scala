package peerless.lib.zquery

import peerless.lib.zquery.NeoDataSource.DataSourceImplSyntax
import zio.query.CompletedRequestMap
import zio.stream.ZStream
import zio.{Chunk, ZIO}
import zio.logging._
import zio.logging.LogAnnotation._
import zio.query.{CompletedRequestMap, DataSource, Request}
import zio.stream.ZStream
import zio.{Chunk, ZIO}

import scala.reflect.ClassTag

trait AllOrPerItem[R, E, Req, Item] {

  /**
   * Data source name
   */
  val name: String

  /**
   * Identifies the 'get all' request
   */
  def isGetAll(request: Req): Boolean

  /**
   * Identifies the 'get one' request
   */
  def isPerItem(request: Req): Boolean

  /**
   * Constructs a 'get all' request
   */
  val allReq: Req

  /**
   * Constructs a 'get one' request for a given item
   */
  def itemToReq(item: Item): ZIO[R, E, Req]

  /**
   * Performs 'get all'
   */
  def getAll(): ZStream[R, E, Item]

  /**
   * Performs 'get some'
   */
  def getSome(reqs: Set[Req]): ZStream[R, E, Item]

  /**
   * Hook to process additional requests not 'get all' or 'get one'
   */
  def processAdditionalRequests(
      requests: Chunk[Req],
      partialResult: CompletedRequestMap
    ): ZIO[R, Nothing, CompletedRequestMap] =
    ZIO.succeed(partialResult)
}

object AllOrPerItem {
  def make[R <: Logging, E, Req <: Request[E, Any], Item](definition: AllOrPerItem[R, E, Req, Item])(implicit reqTag: ClassTag[Req]): DataSource[R, Req] =
    DataSource.Batched.make(definition.name) { (requests: Chunk[Req]) =>

      log.locally(Name(definition.name :: Nil)) {
        val containsAll = requests.exists(definition.isGetAll)
        val byName = requests.filter(definition.isPerItem)

        val baseMap: ZIO[R, E, CompletedRequestMap] = if (containsAll) {
          for {
            _ <- log.info(s"${definition.name} get all")
            foldResult <- definition
              .getAll()
              .foldM((CompletedRequestMap.empty, Set.empty[Item])) { case ((resultMap, all), item) =>
                for {
                  req <- definition.itemToReq(item)
                } yield (resultMap.insert(req)(Right(item)), all + item)
              }
            (perItemMap, allItems) = foldResult
            resultMap = perItemMap.insert(definition.allReq)(Right(allItems))
            _ <- log.info(s"${definition.name} get all completed with ${resultMap.requests.size} items")
          } yield resultMap
        } else {
          ZIO.succeed(CompletedRequestMap.empty)
        }

        baseMap
          .recordFailures(s"${definition.name} get all", requests)
          .flatMap { resultMap =>
            val alreadyHave = resultMap.requests.collect {
              case r: Req if definition.isPerItem(r) => r
            }
            val missing = byName.toSet diff alreadyHave

            val partialResult =
              if (missing.nonEmpty) {
                for {
                  _ <- log.info(s"${definition.name} get (${missing.mkString(", ")})")
                  result <- definition.getSome(missing)
                    .foldM(resultMap) { (resultMap, item) =>
                      for {
                        req <- definition.itemToReq(item)
                      } yield resultMap.insert(req)(Right(item))
                    }
                    .recordFailures("${definition.name} get", missing)
                  _ <- log.info(s"${definition.name} get finished with ${result.requests.size} items")
                } yield result
              } else {
                ZIO.succeed(resultMap)
              }

            partialResult.flatMap(definition.processAdditionalRequests(requests, _))
          }
      }
    }
}

