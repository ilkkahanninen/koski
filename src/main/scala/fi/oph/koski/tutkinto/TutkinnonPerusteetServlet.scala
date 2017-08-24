package fi.oph.koski.tutkinto

import fi.oph.koski.http.{HttpStatus, KoskiErrorCategory}
import fi.oph.koski.koodisto.KoodistoViitePalvelu
import fi.oph.koski.koskiuser.Unauthenticated
import fi.oph.koski.schema.Koodistokoodiviite
import fi.oph.koski.servlet.{ApiServlet, Cached24Hours}

class TutkinnonPerusteetServlet(tutkintoRepository: TutkintoRepository, koodistoViitePalvelu: KoodistoViitePalvelu) extends ApiServlet with Unauthenticated with Cached24Hours {
  get("/oppilaitos/:oppilaitosId") {
   contentType = "application/json;charset=utf-8"
   (params.get("query"), params.get("oppilaitosId")) match {
     case (Some(query), Some(oppilaitosId)) if (query.length >= 3) => tutkintoRepository.findTutkinnot(oppilaitosId, query)
     case _ => KoskiErrorCategory.badRequest.queryParam.searchTermTooShort()
   }
  }

  get("/diaarinumerot/koulutustyyppi/:koulutustyyppi") {
    val koulutusTyyppi = params("koulutustyyppi")
    koodistoViitePalvelu.getSisältyvätKoodiViitteet(koodistoViitePalvelu.getLatestVersion("koskikoulutustendiaarinumerot").get, Koodistokoodiviite(koulutusTyyppi, "koulutustyyppi"))
  }

  get("/tutkinnonosat/:diaari/:suoritustapa") {
    perusteenTutkinnonosat { osa =>
      Right(osa match {
        case None => List.empty
        case Some(rakenneModuuli) => rakenneModuuli match {
          case osa: TutkinnonOsa => List(osa)
          case moduuli: RakenneModuuli => findTutkinnonOsat(moduuli)
        }
      })
    }
  }

  get("/tutkinnonosat/:diaari/:suoritustapa/:ryhma") {
    val ryhmä = params("ryhma")
    val ryhmäkoodi = koodistoViitePalvelu.getKoodistoKoodiViite("ammatillisentutkinnonosanryhma", ryhmä).getOrElse(haltWithStatus(KoskiErrorCategory.badRequest.validation.koodisto.tuntematonKoodi(s"Tuntematon tutkinnon osan ryhmä: $ryhmä")))
    perusteenTutkinnonosat { osa =>
      osa.flatMap(findRyhmä(ryhmäkoodi, _)) match {
        case None =>
          Left(KoskiErrorCategory.notFound.ryhmääEiLöydyRakenteesta())
        case Some(rakennemoduuli) =>
          Right(findTutkinnonOsat(rakennemoduuli))
      }
    }
  }

  get("/suoritustavat/:diaari") {
    val diaari = params("diaari")
    tutkintoRepository.findPerusteRakenne(diaari) match {
      case None => renderStatus(KoskiErrorCategory.notFound.diaarinumeroaEiLöydy("Rakennetta ei löydy diaarinumerolla $diaari"))
      case Some(rakenne) => rakenne.suoritustavat.map(_.suoritustapa)
    }
  }

  private def perusteenTutkinnonosat(f:  Option[RakenneOsa] => Either[HttpStatus, List[TutkinnonOsa]]) = {
    val diaari = params("diaari")
    val suoritustapa = params("suoritustapa")

    tutkintoRepository.findPerusteRakenne(diaari).flatMap(_.suoritustavat.find(_.suoritustapa.koodiarvo == suoritustapa)) match {
      case None =>
        renderStatus(KoskiErrorCategory.notFound.diaarinumeroaEiLöydy(s"Rakennetta ei löydy diaarinumerolla $diaari ja suoritustavalla $suoritustapa"))
      case Some(suoritustapaJaRakenne) =>
        f(suoritustapaJaRakenne.rakenne).right.map(_.map(_.tunniste).distinct.sortBy(_.nimi.map(_.get(lang))))
    }
  }

  private def findRyhmä(ryhmä: Koodistokoodiviite, rakenneOsa: RakenneOsa): Option[RakenneModuuli] = {
    rakenneOsa match {
      case r: RakenneModuuli if r.nimi.get("fi").toLowerCase == ryhmä.nimi.map(_.get("fi")).getOrElse("").toLowerCase =>
        Some(r)
      case r: RakenneModuuli =>
        r.osat.flatMap(findRyhmä(ryhmä, _)).headOption
      case _ => None
    }
  }

  private def findTutkinnonOsat(rakennemoduuli: RakenneModuuli): List[TutkinnonOsa] = rakennemoduuli.osat.flatMap {
    case osa: TutkinnonOsa => List(osa)
    case moduuli: RakenneModuuli => findTutkinnonOsat(moduuli)
  }
}
