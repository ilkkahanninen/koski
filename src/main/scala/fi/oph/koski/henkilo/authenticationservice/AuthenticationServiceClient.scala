package fi.oph.koski.henkilo.authenticationservice

import java.time.LocalDate

import com.typesafe.config.Config
import fi.oph.koski.db.KoskiDatabase.DB
import fi.oph.koski.elasticsearch.ElasticSearch
import fi.oph.koski.http.Http._
import fi.oph.koski.http._
import fi.oph.koski.json.Json4sHttp4s._
import fi.oph.koski.json.JsonSerializer
import fi.oph.koski.koskiuser.Käyttöoikeusryhmät.käyttöoikeusryhmät
import fi.oph.koski.perustiedot.{NimitiedotJaOid, OpiskeluoikeudenPerustiedotRepository}
import fi.oph.koski.schema.Henkilö.Oid
import fi.oph.koski.schema.TäydellisetHenkilötiedot
import fi.oph.koski.util.Timing
import org.http4s._

import scalaz.concurrent.Task
import scalaz.concurrent.Task.gatherUnordered

object RemoteAuthenticationServiceClient {
  def apply(config: Config, perustiedotRepository: => OpiskeluoikeudenPerustiedotRepository, elasticSearch: => ElasticSearch): RemoteAuthenticationServiceClient = {
    val virkailijaUrl: String = if (config.hasPath("authentication-service.virkailija.url")) { config.getString("authentication-service.virkailija.url") } else { config.getString("opintopolku.virkailija.url") }
    val username =  if (config.hasPath("authentication-service.username")) { config.getString("authentication-service.username") } else { config.getString("opintopolku.virkailija.username") }
    val password =  if (config.hasPath("authentication-service.password")) { config.getString("authentication-service.password") } else { config.getString("opintopolku.virkailija.password") }
    val authServiceHttp = VirkailijaHttpClient(username, password, virkailijaUrl, "/authentication-service", config.getBoolean("authentication-service.useCas"))
    val oidServiceHttp = VirkailijaHttpClient(username, password, virkailijaUrl, "/oppijanumerorekisteri-service", config.getBoolean("authentication-service.useCas"))
    val käyttöOikeusHttp = VirkailijaHttpClient(username, password, virkailijaUrl, "/kayttooikeus-service", config.getBoolean("authentication-service.useCas"))
    if (config.hasPath("authentication-service.mockOid") && config.getBoolean("authentication-service.mockOid")) {
      new RemoteAuthenticationServiceClientWithMockOids(authServiceHttp, oidServiceHttp, käyttöOikeusHttp, perustiedotRepository, elasticSearch)
    } else {
      new RemoteAuthenticationServiceClient(authServiceHttp, oidServiceHttp, käyttöOikeusHttp)
    }
  }
}

class RemoteAuthenticationServiceClient(authServiceHttp: Http, oidServiceHttp: Http, käyttöOikeusHttp: Http) extends AuthenticationServiceClient with EntityDecoderInstances with Timing {
  def findOppijaByOid(oid: String): Option[OppijaHenkilö] =
    findOppijatByOids(List(oid)).headOption

  def findOppijatByOids(oids: List[Oid]): List[OppijaHenkilö] =
    runTask(oidServiceHttp.post(uri"/oppijanumerorekisteri-service/henkilo/henkiloPerustietosByHenkiloOidList", oids)(json4sEncoderOf[List[String]])(Http.parseJson[List[OppijaNumerorekisteriOppija]])).map(_.toOppijaHenkilö)

  def findChangedOppijaOids(since: Long): List[Oid] =
    runTask(oidServiceHttp.get(uri"/oppijanumerorekisteri-service/s2s/changedSince/$since?amount=1000")(Http.parseJson[List[String]]))

  def findOppijaByHetu(hetu: String): Option[OppijaHenkilö] =
    runTask(oidServiceHttp.get(uri"/oppijanumerorekisteri-service/henkilo/hetu=$hetu")(Http.parseJsonOptional[OppijaNumerorekisteriOppija])).map(_.toOppijaHenkilö)

  def findMasterOppija(oid: String): Option[OppijaHenkilö] =
    runTask(oidServiceHttp.get(uri"/oppijanumerorekisteri-service/henkilo/$oid/master")(Http.parseJsonOptional[OppijaNumerorekisteriOppija])).map(_.toOppijaHenkilö)

  def findKäyttäjäByOid(oid: String): Option[KäyttäjäHenkilö] = runTask(
    oidServiceHttp.get(uri"/oppijanumerorekisteri-service/henkilo/$oid")(Http.parseJsonOptional[KäyttäjäHenkilö]).flatMap { käyttäjäHenkilö: Option[KäyttäjäHenkilö] =>
      käyttöOikeusHttp.get(uri"/kayttooikeus-service/henkilo/$oid/kayttajatiedot")(Http.parseJsonOptional[Käyttäjätiedot])
        .map(käyttäjätiedot => käyttäjäHenkilö.map(_.copy(kayttajatiedot = käyttäjätiedot)))
    }
  )

  def findOrCreate(createUserInfo: UusiHenkilö): Either[HttpStatus, OppijaHenkilö] =
    runTask(oidServiceHttp.post(uri"/oppijanumerorekisteri-service/s2s/findOrCreateHenkiloPerustieto", createUserInfo)(json4sEncoderOf[UusiHenkilö]) {
      case (x, data, _) if x <= 201 => Right(JsonSerializer.parse[OppijaNumerorekisteriOppija](data, ignoreExtras = true).toOppijaHenkilö)
      case (400, error, _) => Left(KoskiErrorCategory.badRequest.validation.henkilötiedot.virheelliset(error))
      case (status, text, uri) => throw HttpStatusException(status, text, uri)
    })

  def organisaationYhteystiedot(ryhmä: String, organisaatioOid: String): List[Yhteystiedot] = runTask(
    authServiceHttp.get(uri"/authentication-service/resources/henkilo?groupName=$ryhmä&ht=VIRKAILIJA&no=false&org=$organisaatioOid&p=false")(Http.parseJson[HenkilöQueryResult]).flatMap { resp =>
      gatherUnordered(resp.results.map { henkilö =>
        oidServiceHttp.get(uri"/oppijanumerorekisteri-service/henkilo/${henkilö.oidHenkilo}/yhteystiedot/yhteystietotyyppi2")(Http.parseJson[Yhteystiedot])
      })
    }
  )

  def getKäyttöikeusRyhmät: Map[String, List[String]] = runTask(
    käyttöOikeusHttp.get(uri"/kayttooikeus-service/kayttooikeusryhma")(parseJson[List[KäyttöoikeusRyhmä]]).flatMap { ryhmät =>
      gatherUnordered(ryhmät
        .filter(_.description.texts.exists(t => t.lang == "FI" && käyttöoikeusryhmät.map(_.nimi).contains(t.text)))
        .map { ryhmä =>
          käyttöOikeusHttp.get(uri"/kayttooikeus-service/kayttooikeusryhma/${ryhmä.id}/henkilot")(parseJson[KäyttöoikeusRyhmäHenkilöt]).map(h => (ryhmä.nimi, h.personOids))
        }
      )
    }
  ).toMap
}

class RemoteAuthenticationServiceClientWithMockOids(authServiceHttp: Http, oidServiceHttp: Http, käyttöOikeusHttp: Http, perustiedotRepository: OpiskeluoikeudenPerustiedotRepository, elasticSearch: ElasticSearch) extends RemoteAuthenticationServiceClient(authServiceHttp, oidServiceHttp, käyttöOikeusHttp) {
  override def findOppijatByOids(oids: List[String]): List[OppijaHenkilö] = {
    val found = super.findOppijatByOids(oids).map(henkilö => (henkilö.oidHenkilo, henkilö)).toMap
    oids.map { oid =>
      found.get(oid) match {
        case Some(henkilö) => henkilö
        case None =>
          elasticSearch.refreshIndex
          perustiedotRepository.findHenkilöPerustiedot(oid).map { henkilö =>
            OppijaHenkilö(henkilö.oid, henkilö.sukunimi, henkilö.etunimet, henkilö.kutsumanimi, Some("010101-123N"), None, None, None, 0)
          }.getOrElse(OppijaHenkilö(oid, oid.substring("1.2.246.562.24.".length, oid.length), "Testihenkilö", "Testihenkilö", Some("010101-123N"), None, None, None, 0))
      }
    }
  }

  override def findKäyttäjäByOid(oid: String): Option[KäyttäjäHenkilö] = super.findKäyttäjäByOid(oid).orElse {
    Some(KäyttäjäHenkilö(oid, oid.substring("1.2.246.562.24.".length, oid.length), "Tuntematon", "Tuntematon", None))
  }
}

case class KäyttöoikeusTilasto(kokonaismäärä: Int, ryhmienMäärät: Map[String, Int])
case class KäyttöoikeusRyhmä(id: Int, description: KäyttöoikeusRyhmäDescriptions) {
  def nimi = description.texts.find(_.lang == "FI").map(_.text).getOrElse("")
}
case class KäyttöoikeusRyhmäDescriptions(texts: List[KäyttöoikeusRyhmäDescription])
case class KäyttöoikeusRyhmäDescription(text: String, lang: String)
case class KäyttöoikeusRyhmäHenkilöt(personOids: List[String])

object AuthenticationServiceClient {
  def apply(config: Config, db: => DB, perustiedotRepository: => OpiskeluoikeudenPerustiedotRepository, elasticSearch: => ElasticSearch): AuthenticationServiceClient = config.getString("opintopolku.virkailija.url") match {
    case "mock" => new MockAuthenticationServiceClientWithDBSupport(db)
    case _ => RemoteAuthenticationServiceClient(config, perustiedotRepository, elasticSearch)
  }
}

trait AuthenticationServiceClient {
  def findKäyttäjäByOid(oid: String): Option[KäyttäjäHenkilö]
  def findOppijaByOid(oid: String): Option[OppijaHenkilö]
  def findOppijaByHetu(hetu: String): Option[OppijaHenkilö]
  def findOppijatByOids(oids: List[String]): List[OppijaHenkilö]
  def findChangedOppijaOids(since: Long): List[Oid]
  def findMasterOppija(oid: String): Option[OppijaHenkilö]
  def findOrCreate(createUserInfo: UusiHenkilö): Either[HttpStatus, OppijaHenkilö]
  def organisaationYhteystiedot(ryhmä: String, organisaatioOid: String): List[Yhteystiedot]
  def getKäyttöikeusRyhmät: Map[String, List[String]]

  def henkilötPerKäyttöoikeusryhmä: KäyttöoikeusTilasto = {
    val ryhmät = getKäyttöikeusRyhmät
    KäyttöoikeusTilasto(
      ryhmät.values.flatten.toList.distinct.size,
      ryhmät.map { case (x, y) => (x, y.size) }
    )
  }
}

case class HenkilöQueryResult(totalCount: Int, results: List[QueryHenkilö])
case class QueryHenkilö(oidHenkilo: String, sukunimi: String, etunimet: String, kutsumanimi: String, hetu: Option[String])
case class OppijaNumerorekisteriOppija(oidHenkilo: String, sukunimi: String, etunimet: String, kutsumanimi: String, hetu: Option[String], syntymaaika: Option[LocalDate], aidinkieli: Option[Kieli], kansalaisuus: Option[List[Kansalaisuus]], modified: Long) {
  def toOppijaHenkilö = OppijaHenkilö(oidHenkilo, sukunimi, etunimet, kutsumanimi, hetu, syntymaaika, aidinkieli.map(_.kieliKoodi), kansalaisuus.map(_.map(_.kansalaisuusKoodi)), modified)
}

case class Kieli(kieliKoodi: String)
case class Kansalaisuus(kansalaisuusKoodi: String)
case class OppijaHenkilö(oidHenkilo: String, sukunimi: String, etunimet: String, kutsumanimi: String, hetu: Option[String], syntymaika: Option[LocalDate], aidinkieli: Option[String], kansalaisuus: Option[List[String]], modified: Long) {
  def toQueryHenkilö = QueryHenkilö(oidHenkilo, sukunimi, etunimet, kutsumanimi, hetu)
  def toTäydellisetHenkilötiedot = TäydellisetHenkilötiedot(oidHenkilo, etunimet, kutsumanimi, sukunimi)
  def toNimitiedotJaOid = NimitiedotJaOid(oidHenkilo, etunimet, kutsumanimi, sukunimi)
}

case class Yhteystiedot(sahkoposti: String)
case class KäyttäjäHenkilö(oidHenkilo: String, sukunimi: String, etunimet: String, kutsumanimi: String, kayttajatiedot: Option[Käyttäjätiedot])
case class UusiHenkilö(hetu: Option[String], sukunimi: String, etunimet: String, kutsumanimi: String, henkiloTyyppi: String, kayttajatiedot: Option[Käyttäjätiedot])
case class Käyttäjätiedot(username: Option[String])

object UusiHenkilö {
  def palvelu(nimi: String) = UusiHenkilö(None, nimi, "_", "_", "PALVELU", Some(Käyttäjätiedot(Some(nimi))))
  def oppija(hetu: Option[String], sukunimi: String, etunimet: String, kutsumanimi: String) = UusiHenkilö(hetu, sukunimi, etunimet, kutsumanimi, "OPPIJA", None)
}

case class OrganisaatioHenkilö(organisaatioOid: String, passivoitu: Boolean)
case class UusiKäyttöoikeusryhmä(ryhmaNameFi: String, ryhmaNameSv: String, ryhmaNameEn: String, palvelutRoolit: List[Palvelurooli] = Nil, organisaatioTyypit: List[String] = Nil, slaveIds: List[Void] = Nil)

case class Palvelurooli(palveluName: String, rooli: String)
object Palvelurooli {
  def apply(rooli: String): Palvelurooli = Palvelurooli("KOSKI", rooli)
}
case class YhteystietoRyhmä(id: Int, ryhmaKuvaus: String, yhteystiedot: List[Yhteystieto])
case class Yhteystieto(yhteystietoTyyppi: String, yhteystietoArvo: String)