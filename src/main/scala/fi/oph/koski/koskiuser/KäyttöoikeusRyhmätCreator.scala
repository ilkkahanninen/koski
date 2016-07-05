package fi.oph.koski.koskiuser

import com.typesafe.config.Config
import fi.oph.koski.henkilo.{AuthenticationServiceClient, UusiKäyttöoikeusryhmä}
import fi.oph.koski.koodisto.KoodistoPalvelu
import fi.oph.koski.organisaatio.Opetushallitus

object KäyttöoikeusRyhmätCreator {
  def luoKäyttöoikeusRyhmät(config: Config): Unit = {
    val client: AuthenticationServiceClient = AuthenticationServiceClient(config)
    val olemassaOlevatRyhmät = client.käyttöoikeusryhmät
    val koodistopalvelu: KoodistoPalvelu = KoodistoPalvelu(config)
    val oppilaitostyypit: List[String] = koodistopalvelu.getLatestVersion("oppilaitostyyppi").flatMap(koodistopalvelu.getKoodistoKoodit(_)).toList.flatten.map(_.koodiArvo)

    Käyttöoikeusryhmät.käyttöoikeusryhmät foreach { ryhmä =>
      val olemassaOlevaRyhmä = olemassaOlevatRyhmät.find(olemassaOlevaRyhmä => olemassaOlevaRyhmä.toKoskiKäyttöoikeusryhmä.map(_.nimi) == Some(ryhmä.nimi))
      val organisaatioTyypit = (ryhmä.orgAccessType, ryhmä.globalAccessType) match {
        case (Nil, Nil) => Nil // käyttöoikeusryhmä ei liity mihinkään
        case (Nil, _) => List(Opetushallitus.organisaatioOid) // global access => liitetään OPH-organisaatioon
        case (_, Nil) => oppilaitostyypit // käyttöoikeusryhmä liittyy oppilaitoksiin, eikä sisällä yleistä pääsyä
        case _ => throw new IllegalArgumentException("Ei voi olla molempia pääsyjä")
      }
      val tiedot = UusiKäyttöoikeusryhmä(ryhmä.nimi, ryhmä.nimi, ryhmä.nimi, organisaatioTyypit = organisaatioTyypit)
      olemassaOlevaRyhmä match {
        case Some(o) =>
          println("päivitetään " + ryhmä)
          client.muokkaaKäyttöoikeusryhmä(o.id, tiedot)
        case None =>
          println("luodaan " + ryhmä)
          client.luoKäyttöoikeusryhmä(tiedot)
      }
    }
  }
}
