package fi.oph.koski.mocha

import java.io.{InputStream, OutputStream}

import fi.oph.koski.log.Logging
import fi.oph.koski.util.Streams
import org.scalatest.{FreeSpec, Matchers}

import scala.sys.process._

trait KoskiCommandLineSpec extends FreeSpec with Matchers with Logging {
  def runTestCommand(description: String, command: Seq[String]): Unit = {
    logger.info(s"running $description")

    val io = new ProcessIO(_.close, pipeTo(System.out), pipeTo(System.err))

    val res: Process = command.run(io)
    if (res.exitValue() != 0) {
      logger.info(s"$description failed")
    }
    res.exitValue() should equal (0)
  }

  private def pipeTo(dest: OutputStream)(is: InputStream): Unit = Streams.pipeTo(is, dest)
}
