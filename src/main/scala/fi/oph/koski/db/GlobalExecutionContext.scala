package fi.oph.koski.db

import cats.effect.{ContextShift, IO, Timer}
import fi.oph.koski.executors.Pools

import scala.concurrent.ExecutionContextExecutor

trait GlobalExecutionContext {
  implicit val executor: ExecutionContextExecutor = Pools.globalExecutor
  implicit val timer: Timer[IO] = IO.timer(executor)
  implicit val cs: ContextShift[IO] = IO.contextShift(executor)
}
