package fi.oph.koski.localization

import fi.oph.koski.cache.GlobalCacheManager
import fi.oph.koski.json.JsonFiles
import fi.oph.koski.json.JsonSerializer.extract
import fi.oph.koski.valpas.localization.ValpasLocalizationConfig
import org.json4s.{JArray, JObject, JValue}

// VIRKAILIJA_ROOT=https://virkailija.opintopolku.fi mvn scala:testCompile exec:java -Dexec.mainClass=fi.oph.koski.localization.LocalizationMockDataUpdater
object LocalizationMockDataUpdater extends App {
  lazy val localizationConfig = sys.env.getOrElse("LOCALIZATION_CATEGORY", "koski") match {
    case "koski" => new KoskiLocalizationConfig
    case "valpas" => new ValpasLocalizationConfig
  }
  val filename = "src/main/resources" + localizationConfig.mockLocalizationResourceFilename
  val root = sys.env.getOrElse("VIRKAILIJA_ROOT", throw new RuntimeException("Environment variable VIRKAILIJA_ROOT missing"))

  val JArray(localizations) = new ReadOnlyRemoteLocalizationRepository(root, localizationConfig)(GlobalCacheManager).fetchLocalizations.asInstanceOf[JArray]
  //val JArray(localizations) = JsonFiles.readFile(filename)
  val sorted = stabilize(localizations)
  JsonFiles.writeFile(filename, sorted)

  private def stabilize(entries: Seq[JValue]) = {
    entries
      .sortBy {
        case j: JObject => (extract[String](j \ "key"), extract[String](j \ "locale"))
        case _ => ???
      }
      .map {
        case JObject(fields) => JObject(fields.sortBy(_._1))
        case _ => ???
      }
  }
}

