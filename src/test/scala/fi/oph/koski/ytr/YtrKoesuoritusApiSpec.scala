package fi.oph.koski.ytr

import java.io.InputStream

import fi.oph.koski.api.{LocalJettyHttpSpecification, OpiskeluoikeusTestMethods}
import fi.oph.koski.util.ClasspathResources.readResourceIfExists
import fi.oph.scalaschema.SchemaValidatingExtractor
import org.json4s.jackson.JsonMethods
import org.scalatest.FreeSpec

import scala.collection.Iterator.continually

class YtrKoesuoritusApiSpec extends FreeSpec with LocalJettyHttpSpecification with OpiskeluoikeusTestMethods {
  "Kansalainen" - {
    "voi hakea koesuorituslistauksen" in {
      get("api/ytrkoesuoritukset", headers = kansalainenLoginHeaders("080698-967F")) {
        verifyResponseStatusOk()
        readExams should equal (expected)
      }
    }
  }

  "Viranomainen" - {
    "ei voi hakea koesuorituslistausta" in {
      authGet("api/ytrkoesuoritukset", defaultUser) {
        verifyResponseStatus(403, Nil)
      }
    }
  }

  private def resourceAsByteArray(resourceName: String): Array[Byte] =
    readResourceIfExists(resourceName, (is: InputStream) => continually(is.read).takeWhile(_ != -1).map(_.toByte).toArray).get

  import fi.oph.koski.schema.KoskiSchema.deserializationContext
  private def readExams: List[ExamResponse] =
    SchemaValidatingExtractor.extract[List[ExamResponse]](JsonMethods.parse(body)).right.get

  private val expected = List(
    ExamResponse(period = "2012K", examId = "A", copyOfExamPaper = Some("2345K_XX_12345.pdf")),
    ExamResponse(period = "2012K", examId = "BB", copyOfExamPaper = Some("not-found-from-s3.pdf")),
    ExamResponse(period = "2012K", examId = "EA"),
    ExamResponse(period = "2012K", examId = "GE"),
    ExamResponse(period = "2012K", examId = "N")
  )
}
