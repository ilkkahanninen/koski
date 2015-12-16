package fi.oph.tor.schema

import fi.oph.tor.schema.generic._
import org.json4s.JsonAST._

import scala.annotation.StaticAnnotation

case class KoodistoUri(koodistoUri: String) extends StaticAnnotation with Metadata {
  def asLink = <a href={"/tor/documentation/koodisto/" + koodistoUri + "/latest"} target="_blank">{koodistoUri}</a>
}

object KoodistoUri extends MetadataSupport {
  override val applyAnnotations: PartialFunction[(String, List[String], ObjectWithMetadata[_], SchemaFactory), ObjectWithMetadata[_]] = {
    case (annotationClass, params, property: Property, schemaFactory) if (annotationClass == classOf[KoodistoUri].getName) =>
      val koodistoUri = KoodistoUri(params.mkString(" "))
      val finalInnerSchema = property.schema.mapItems { itemSchema =>
        val koodistoViiteSchema: ClassSchema = toKoodistoKoodiViiteSchema(schemaFactory, itemSchema)
        koodistoViiteSchema.copy(properties = koodistoViiteSchema.properties.map{
          case p if p.key == "koodistoUri" => addEnumValue(koodistoUri.koodistoUri, p)
          case p => p
        })
      }
      property.copy(schema = finalInnerSchema).appendMetadata(List(koodistoUri))
  }

  def toKoodistoKoodiViiteSchema(schemaFactory: SchemaFactory, itemSchema: Schema): ClassSchema = {
    val koodistoViiteSchema: ClassSchema = itemSchema match {
      case s: ClassSchema => s
      case s: ClassRefSchema => schemaFactory.createSchema(classOf[KoodistoKoodiViite].getName).asInstanceOf[ClassSchema]
    }
    koodistoViiteSchema
  }

  override def appendMetadataToJsonSchema(obj: JObject, metadata: Metadata) = metadata match {
    case k: KoodistoUri => appendToDescription(obj, "(Koodisto: " + k.asLink + ")")
    case _ => obj
  }
}
