package fi.oph.koski.koskiuser

import fi.oph.koski.cache.{CachingStrategy, KeyValueCache}
import fi.oph.koski.henkilo.{MockAuthenticationServiceClient, AuthenticationServiceClient}
import fi.oph.koski.organisaatio.{MockOrganisaatioRepository, Opetushallitus, OrganisaatioHierarkia, OrganisaatioRepository}
import fi.oph.koski.util.Timing
import rx.lang.scala.Observable

class KäyttöoikeusRepository(authenticationServiceClient: AuthenticationServiceClient, organisaatioRepository: OrganisaatioRepository) extends Timing {
  def käyttäjänKäyttöoikeudet(oid: String): Observable[Set[Käyttöoikeus]] = käyttöoikeusCache(oid)

  def käyttäjänOppilaitostyypit(oid: String): Set[String] = käyttöoikeusCache(oid).toBlocking.first
    .filter(_.ryhmä.orgAccessType.contains(AccessType.read))
    .flatMap(_.oppilaitostyyppi)

  private lazy val käyttöoikeusryhmätCache = authenticationServiceClient.käyttöoikeusryhmät
  private def ryhmäById(ryhmäId: Int) = käyttöoikeusryhmätCache.find(_.id == ryhmäId).flatMap(_.toKoskiKäyttöoikeusryhmä)
  private def haeKäyttöoikeudet(henkilöOid: String): Observable[Set[Käyttöoikeus]] = timedObservable("käyttäjänOrganisaatiot")(authenticationServiceClient.käyttäjänKäyttöoikeusryhmät(henkilöOid)
    .map { (käyttöoikeudet: List[(String, Int)]) =>
      käyttöoikeudet.toSet.flatMap { tuple: (String, Int) =>
        tuple match {
          case (organisaatioOid: String, ryhmäId: Int) =>
            organisaatioOid match {
              case Opetushallitus.organisaatioOid =>
                ryhmäById(ryhmäId).flatMap{
                  case r: GlobaaliKäyttöoikeusryhmä => Some(GlobaaliKäyttöoikeus(r))
                  case r: OrganisaationKäyttöoikeusryhmä =>
                    logger.warn(s"Käyttäjällä $henkilöOid on organisaatiotyyppinen käyttöoikeusryhmä $r liitettynä OPH-organisaatioon")
                    None
                }.toList
              case _ =>
                def flatten(orgs: List[OrganisaatioHierarkia]): List[OrganisaatioHierarkia] = {
                  orgs ++ orgs.flatMap { org => org :: flatten(org.children) }
                }
                val organisaatioHierarkia: Option[OrganisaatioHierarkia] = organisaatioRepository.getOrganisaatioHierarkia(organisaatioOid)

                val flattened: List[OrganisaatioHierarkia] = organisaatioHierarkia match {
                  case Some(organisaatioHierarkia) => flatten(List(organisaatioHierarkia))
                  case None =>
                    logger.warn(s"Käyttäjän $henkilöOid käyttöoikeus $ryhmäId kohdistuu organisaatioon $organisaatioOid, jota ei löydy")
                    Nil
                }

                flattened.flatMap { org =>
                  val käyttöoikeus: Option[OrganisaatioKäyttöoikeus] = ryhmäById(ryhmäId).flatMap {
                    case r: GlobaaliKäyttöoikeusryhmä =>
                      logger.warn(s"Käyttäjällä $henkilöOid on globaali käyttöoikeusryhmä $r liitettynä organisaatioon $organisaatioOid")
                      None
                    case r: OrganisaationKäyttöoikeusryhmä =>
                      Some(OrganisaatioKäyttöoikeus(org.toOrganisaatio, org.oppilaitostyyppi, r))
                  }
                  käyttöoikeus
                }
            }
        }
      }
    })
  private lazy val käyttöoikeusCache = new KeyValueCache[String, Observable[Set[Käyttöoikeus]]](
    CachingStrategy.cacheAllNoRefresh("userOrganisations", 3600, 100), haeKäyttöoikeudet
  )
}

object MockKäyttöoikeusRepository extends KäyttöoikeusRepository(new MockAuthenticationServiceClient(), MockOrganisaatioRepository)