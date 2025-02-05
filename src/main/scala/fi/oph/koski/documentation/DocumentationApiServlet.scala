package fi.oph.koski.documentation

import fi.oph.koski.http.KoskiErrorCategory
import fi.oph.koski.json.JsonSerializer
import fi.oph.koski.kela.KelaSchema
import fi.oph.koski.koodisto.Koodistot
import fi.oph.koski.koskiuser.Unauthenticated
import fi.oph.koski.migri.MigriSchema
import fi.oph.koski.schema.KoskiSchema
import fi.oph.koski.servlet.{KoskiSpecificApiServlet, NoCache}
import fi.oph.koski.valvira.ValviraSchema

import scala.reflect.runtime.{universe => ru}

class DocumentationApiServlet extends KoskiSpecificApiServlet with Unauthenticated with NoCache {
  get("/categoryNames.json") {
    KoskiTiedonSiirtoHtml.categoryNames
  }

  get("/categoryExampleMetadata.json") {
    KoskiTiedonSiirtoHtml.categoryExamples
  }

  get("/categoryExamples/:category/:name/table.html") {
    renderOption(KoskiErrorCategory.notFound)(KoskiTiedonSiirtoHtml.jsonTableHtmlContents(params("category"), params("name")))
  }

  get("/sections.html") {
    KoskiTiedonSiirtoHtml.htmlTextSections
  }

  get("/apiOperations.json") {
    KoskiTiedonSiirtoHtml.apiOperations
  }

  get("/examples/:name.json") {
    renderOption(KoskiErrorCategory.notFound)(Examples.allExamples.find(_.name == params("name")).map(_.data))
  }
  get("/koski-oppija-schema.json") {
    KoskiSchema.schemaJson
  }

  get("/valvira-oppija-schema.json") {
    ValviraSchema.schemaJson
  }

  get("/kela-oppija-schema.json") {
    KelaSchema.schemaJson
  }

  get("/migri-oppija-schema.json") {
    MigriSchema.schemaJson
  }

  get("/koodistot.json") {
    renderObject[List[String]](Koodistot.koodistot)
  }

  override def toJsonString[T: ru.TypeTag](x: T): String = JsonSerializer.writeWithRoot(x)
}
