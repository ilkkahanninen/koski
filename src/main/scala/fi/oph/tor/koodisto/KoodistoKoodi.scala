package fi.oph.tor.koodisto

import java.time.LocalDate

import fi.oph.tor.localization.LocalizedString

case class KoodistoKoodi(koodiUri: String, koodiArvo: String, metadata: List[KoodistoKoodiMetadata], versio: Int, voimassaAlkuPvm: Option[LocalDate]) {
  private def localizedStringFromMetadata(f: KoodistoKoodiMetadata => Option[String]): LocalizedString = {
    val values: Map[String, String] = metadata.flatMap { meta => f(meta).flatMap { nimi => meta.kieli.map { kieli => (kieli, nimi) } } }.toMap
    LocalizedString(values)
  }

  def nimi: LocalizedString = localizedStringFromMetadata { meta => meta.nimi }

  def lyhytNimi: LocalizedString = localizedStringFromMetadata { meta => meta.lyhytNimi }

  def getMetadata(kieli: String): Option[KoodistoKoodiMetadata] = {
    metadata.find(_.kieli == Some(kieli.toUpperCase))
  }

}