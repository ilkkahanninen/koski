package fi.oph.koski.documentation

import fi.oph.koski.localization.LocalizedString
import fi.oph.koski.schema._
import fi.oph.koski.util.Files
import fi.oph.scalaschema._
import fi.oph.scalaschema.annotation._

import scala.collection.mutable.MutableList
import scala.xml.Elem

object KoskiSchemaDocumentHtml {
  def mainSchema = KoskiSchema.schema
  def html(shallowEntities: List[Class[_]] = Nil, focusEntitySimplename: Option[String] = None) = {
    <html>
      <head>
        <link type="text/css" rel="stylesheet" href="/koski/css/schema-printable.css"/>
      </head>
      <body>
        <h1>Koski-tietomalli</h1>
        { schemaHtml(mainSchema, shallowEntities, focusEntitySimplename) }
      </body>
    </html>
  }

  def schemaHtml(schema: ClassSchema, shallowEntities: List[Class[_]], focusEntitySimplename: Option[String]) = {
    val backlog = buildBacklog(mainSchema, new MutableList[String], shallowEntities, focusEntitySimplename).toList
    backlog.map(name => mainSchema.getSchema(name).get.asInstanceOf[ClassSchema]).map(s => classHtml(s, backlog))
  }

  private def buildBacklog(x: ClassSchema, backlog: MutableList[String], shallowEntities: List[Class[_]], focusEntitySimplename: Option[String]): MutableList[String] = {
    val name = x.fullClassName
    if (!backlog.contains(name)) {
      backlog += name
      if (!shallowEntities.map(_.getName).contains(name)) {
        val moreSchemas: Seq[ClassSchema] = x.properties.flatMap { p =>
          val (itemSchema, _) = cardinalityAndItemSchema(p.schema, p.metadata)
          val resolvedItemSchema = resolveSchema(itemSchema)
          classSchemasIn(resolvedItemSchema)
        }.filter(s => focusEntitySimplename.isEmpty || focusEntitySimplename.get == s.simpleName)

        moreSchemas.foreach { s =>
          buildBacklog(s, backlog, shallowEntities, None)
        }
      }
    }
    backlog
  }

  private def classSchemasIn(schema: Schema): List[ClassSchema] = schema match {
    case s: ClassSchema => List(s)
    case s: AnyOfSchema => s.alternatives.map {
      case s: ClassSchema => s
      case s: ClassRefSchema => resolveSchema(s).asInstanceOf[ClassSchema]
    }
    case _ => Nil
  }


  def classHtml(schema: ClassSchema, includedEntities: List[String]) = <div class="entity">
    <h3 id={schema.simpleName}>{schema.title}</h3>
    {descriptionHtml(schema)}
    <table>
      <thead>
        <tr>
          <th class="nimi">Nimi</th>
          <th class="lukumäärä">Lukumäärä</th>
          <th class="tyyppi">Tyyppi</th>
          <th class="kuvaus">Kuvaus</th>
        </tr>
      </thead>
      <tbody>
        {
          schema.properties.map { p =>
            val (itemSchema, cardinality) = cardinalityAndItemSchema(p.schema, p.metadata)
            val resolvedItemSchema = resolveSchema(itemSchema)
            <tr>
              <td class="nimi">{p.title}</td>
              <td class="lukumäärä">{cardinality}</td>
              <td class="tyyppi">
                {schemaTypeHtml(resolvedItemSchema, includedEntities)}
                {metadataHtml(p.metadata ++ p.schema.metadata)}
              </td>
              <td class="kuvaus">
                {descriptionHtml(p)}
              </td>
            </tr>
          }
        }
      </tbody>
    </table>
  </div>

  def schemaTypeHtml(s: Schema, includedEntities: List[String]): Elem = s match {
    case s: ClassSchema => <a href={if (includedEntities.contains(s.fullClassName)) {"#" + s.simpleName} else { "?entity=" + s.simpleName }}>{s.title}</a>
    case s: AnyOfSchema => <span class={"alternatives " + s.simpleName}>{s.alternatives.map(a => schemaTypeHtml(resolveSchema(a), includedEntities))}</span>
    case s: StringSchema => <span>merkkijono</span> // TODO: schemarajoitukset annotaatioista jne
    case s: NumberSchema => <span>numero</span>
    case s: BooleanSchema => <span>true/false</span>
    case s: DateSchema => <span>päivämäärä</span>
  }

  private def resolveSchema(schema: Schema): Schema = schema match {
    case s: ClassRefSchema => mainSchema.getSchema(s.fullClassName).get
    case _ => schema
  }

  private def cardinalityAndItemSchema(s: Schema, metadata: List[Metadata]):(ElementSchema, Cardinality) = s match {
    case s@ListSchema(itemSchema) => (itemSchema.asInstanceOf[ElementSchema], Cardinality(minItems(s, metadata), maxItems(s, metadata)))
    case OptionalSchema(i: ListSchema) =>
      val (itemSchema, Cardinality(min, max)) = cardinalityAndItemSchema(i, metadata)
      (itemSchema, Cardinality(0, max))
    case OptionalSchema(itemSchema: ElementSchema) =>
      (itemSchema, Cardinality(0, Some(1)))
    case s: ElementSchema => (s, Cardinality(1, Some(1)))
  }

  private def minItems(s: ListSchema, metadata: List[Metadata]): Int = (metadata ++ s.metadata).collect {
    case MinItems(min) => min
  }.headOption.getOrElse(0)

  private def maxItems(s: ListSchema, metadata: List[Metadata]): Option[Int] = (metadata ++ s.metadata).collect {
    case MaxItems(max) => max
  }.headOption

  private def metadataHtml(metadatas: List[Metadata]) = {
    {
      metadatas.flatMap {
        case k: KoodistoUri =>Some(<div class="koodisto">Koodisto: {k.asLink}</div>)
        case k: KoodistoKoodiarvo =>Some(<div class="koodiarvo">Hyväksytty koodiarvo: {k.arvo}</div>)
        case o: OksaUri => Some(<div class="oksa">Oksa: {o.asLink}</div>)
        case _ => None
      }
    }
  }

  private def descriptionHtml(p: Property): List[Elem] = descriptionHtml((p.metadata ++ p.schema.metadata))
  private def descriptionHtml(p: ObjectWithMetadata[_]): List[Elem] = descriptionHtml(p.metadata)
  private def descriptionHtml(metadata: List[Metadata]): List[Elem] = metadata flatMap {
    case Description(desc) => Some(<span class="description">{desc}</span>)
    case ReadOnly(desc) => Some(<div class="readonly">{desc}</div>)
    case _ => None
  }


  case class Cardinality(min: Int, max: Option[Int]) {
    override def toString: String = (min, max) match {
      case (1, Some(1)) => "1"
      case (min, Some(max)) => s"$min..$max"
      case (min, None) => s"$min..n"
    }
  }
}

object KoskiSchemaDocumentHtmlPrinter extends App {
  Files.writeFile("main-schema.html", KoskiSchemaDocumentHtml.html(
    shallowEntities = List(classOf[Oppija])).toString)
  Files.writeFile("ammatillinen-schema.html", KoskiSchemaDocumentHtml.html(
    shallowEntities = List(classOf[OsaamisenTunnustaminen]),
    focusEntitySimplename = Some("ammatillinenopiskeluoikeus")).toString)
}