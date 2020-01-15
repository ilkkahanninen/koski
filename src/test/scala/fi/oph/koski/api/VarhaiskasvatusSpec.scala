package fi.oph.koski.api

import fi.oph.koski.documentation.ExamplesEsiopetus.{ostopalvelu, päiväkodinEsiopetuksenTunniste}
import fi.oph.koski.documentation.YleissivistavakoulutusExampleData.päiväkotiTouhula
import fi.oph.koski.henkilo.MockOppijat.{asUusiOppija, ysiluokkalainen}
import fi.oph.koski.http.KoskiErrorCategory
import fi.oph.koski.koskiuser.MockUsers
import fi.oph.koski.organisaatio.MockOrganisaatiot
import fi.oph.koski.schema.EsiopetuksenOpiskeluoikeus
import org.scalatest.FreeSpec

class VarhaiskasvatusSpec extends FreeSpec with EsiopetusSpecification {
  "Varhaiskasvatuksen järjestäjä koulutustoimija" - {
    "kun järjestämismuoto syötetty" - {
      "voi luoda, lukea, päivittää ja mitätöidä päiväkodissa järjestettävän esiopetuksen opiskeluoikeuden organisaatiohierarkiansa ulkopuolelta" in {
        val opiskeluoikeus = päiväkotiEsiopetus(päiväkotiTouhula, ostopalvelu)
        val resp = putOpiskeluoikeus(opiskeluoikeus, headers = authHeaders(MockUsers.helsinkiTallentaja) ++ jsonContent) {
          verifyResponseStatusOk()
          readPutOppijaResponse
        }

        val oo = resp.opiskeluoikeudet.head
        oo.versionumero should equal(1)

        authGet(s"api/opiskeluoikeus/${oo.oid}", user = MockUsers.helsinkiTallentaja) {
          verifyResponseStatusOk()
        }

        authGet(s"api/oppija/${resp.henkilö.oid}", user = MockUsers.helsinkiTallentaja) {
          verifyResponseStatusOk()
        }

        putOpiskeluoikeus(opiskeluoikeus.copy(oid = Some(oo.oid), lisätiedot = None), headers = authHeaders(MockUsers.helsinkiTallentaja) ++ jsonContent) {
          verifyResponseStatusOk()
          readPutOppijaResponse.opiskeluoikeudet.head.versionumero should equal(2)
        }

        delete(s"api/opiskeluoikeus/${resp.opiskeluoikeudet.head.oid}", headers = authHeaders(MockUsers.helsinkiTallentaja)) {
          verifyResponseStatusOk()
        }
      }

      "voi siirtää koulutustoimijatiedon" in {
        val opiskeluoikeus = päiväkotiEsiopetus(päiväkotiTouhula, ostopalvelu).copy(koulutustoimija = hki)
        putOpiskeluoikeus(opiskeluoikeus, headers = authHeaders(MockUsers.helsinkiTallentaja) ++ jsonContent) {
          verifyResponseStatusOk()
        }
      }

      "ei voi luoda perusopetuksessa järjestettävien esiopetuksen opiskeluoikeuksia organisaatiohierarkiansa ulkopuolelle" in {
        putOpiskeluoikeus(peruskouluEsiopetus(päiväkotiTouhula, ostopalvelu), headers = authHeaders(MockUsers.helsinkiTallentaja) ++ jsonContent) {
          verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.koodisto.vääräkoulutustyyppi(s"Järjestämismuoto sallittu vain päiväkodissa järjestettävälle esiopetukselle ($päiväkodinEsiopetuksenTunniste)"))
        }
      }

      "ei voi lukea, päivittää tai poistaa muiden luomia opiskeluoikeuksia organisaatiohierarkiansa ulkopuolelta" in {
        val esiopetuksenOpiskeluoikeus: EsiopetuksenOpiskeluoikeus = päiväkotiEsiopetus(päiväkotiTouhula, ostopalvelu)
        val resp = putOpiskeluoikeus(esiopetuksenOpiskeluoikeus, headers = authHeaders(MockUsers.helsinkiTallentaja) ++ jsonContent) {
          verifyResponseStatusOk()
          readPutOppijaResponse
        }
        val oo = resp.opiskeluoikeudet.head

        authGet(s"api/opiskeluoikeus/${oo.oid}", user = MockUsers.tornioTallentaja) {
          verifyResponseStatus(404, KoskiErrorCategory.notFound.opiskeluoikeuttaEiLöydyTaiEiOikeuksia())
        }

        authGet(s"api/oppija/${resp.henkilö.oid}", user = MockUsers.tornioTallentaja) {
          verifyResponseStatus(404, KoskiErrorCategory.notFound.oppijaaEiLöydyTaiEiOikeuksia("Oppijaa 1.2.246.562.24.00000000001 ei löydy tai käyttäjällä ei ole oikeuksia tietojen katseluun."))
        }

        putOpiskeluoikeus(esiopetuksenOpiskeluoikeus.copy(oid = Some(oo.oid), lisätiedot = None), headers = authHeaders(MockUsers.tornioTallentaja) ++ jsonContent) {
          verifyResponseStatus(404, KoskiErrorCategory.notFound.opiskeluoikeuttaEiLöydyTaiEiOikeuksia(s"Opiskeluoikeutta ${oo.oid} ei löydy tai käyttäjällä ei ole oikeutta sen katseluun"))
        }

        delete(s"api/opiskeluoikeus/${resp.opiskeluoikeudet.head.oid}", headers = authHeaders(MockUsers.tornioTallentaja)) {
          verifyResponseStatus(404, KoskiErrorCategory.notFound.opiskeluoikeuttaEiLöydyTaiEiOikeuksia())
        }
      }
    }

    "kun järjestämismuotoa ei syötetty" - {
      "ei voi luoda opiskeluoikeuksia organisaatiohierarkian ulkopuolelle" in {
        val opiskeluoikeus = päiväkotiEsiopetus(päiväkotiTouhula).copy(koulutustoimija = hki)
        putOpiskeluoikeus(opiskeluoikeus, headers = authHeaders(MockUsers.helsinkiTallentaja) ++ jsonContent) {
          verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.organisaatio.vääräKoulutustoimija(s"Annettu koulutustoimija ${MockOrganisaatiot.helsinginKaupunki} ei vastaa organisaatiopalvelusta löytyvää koulutustoimijaa ${MockOrganisaatiot.pyhtäänKunta}"))
        }
      }

      "ei voi siirtää muiden luomia opiskeluoikeuksia organisaatiohierarkiansa ulkopuolelta oman organisaationsa alle" in {
        val esiopetuksenOpiskeluoikeus: EsiopetuksenOpiskeluoikeus = päiväkotiEsiopetus(päiväkotiTouhula, ostopalvelu)
        val opiskeluoikeusOid = putOpiskeluoikeus(esiopetuksenOpiskeluoikeus, headers = authHeaders(MockUsers.helsinkiTallentaja) ++ jsonContent) {
          verifyResponseStatusOk()
          readPutOppijaResponse.opiskeluoikeudet.head.oid
        }

        putOpiskeluoikeus(esiopetuksenOpiskeluoikeus.copy(oid = Some(opiskeluoikeusOid), koulutustoimija = tornio), headers = authHeaders(MockUsers.tornioTallentaja) ++ jsonContent) {
          verifyResponseStatus(404, KoskiErrorCategory.notFound.opiskeluoikeuttaEiLöydyTaiEiOikeuksia(s"Opiskeluoikeutta $opiskeluoikeusOid ei löydy tai käyttäjällä ei ole oikeutta sen katseluun"))
        }
      }
    }
  }

  "Varhaiskasvatuksen järjestäjä kahden koulutustoimijan käyttäjä, järjestämismuoto syötetty" - {
    "voi luoda ja muokata esiopetuksen opiskeluoikeuden organisaatiohierarkiansa ulkopuolelle jos koulutustoimija on syötetty" in {
      val opiskeluoikeus = päiväkotiEsiopetus(päiväkotiTouhula, ostopalvelu).copy(koulutustoimija = hki)
      val resp = putOpiskeluoikeus(opiskeluoikeus, headers = authHeaders(MockUsers.helsinkiSekäTornioTallentaja) ++ jsonContent) {
        verifyResponseStatusOk()
        readPutOppijaResponse
      }

      putOpiskeluoikeus(opiskeluoikeus.copy(oid = Some(resp.opiskeluoikeudet.head.oid), koulutustoimija = tornio), headers = authHeaders(MockUsers.helsinkiSekäTornioTallentaja) ++ jsonContent) {
        verifyResponseStatusOk()
      }
    }

    "ei voi luoda ilman koulutustoimijatietoa" in {
      val opiskeluoikeus = päiväkotiEsiopetus(päiväkotiTouhula, ostopalvelu)
      putOpiskeluoikeus(opiskeluoikeus, headers = authHeaders(MockUsers.helsinkiSekäTornioTallentaja) ++ jsonContent) {
        verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.organisaatio.koulutustoimijaPakollinen(s"Koulutustoimijaa ei voi yksiselitteisesti päätellä käyttäjätunnuksesta. Koulutustoimija on pakollinen."))
      }
    }
  }

  "Koulutustoimija joka ei ole varhaiskasvatuksen järjestäjä" - {
    "ei voi luoda päiväkodissa järjestettävän esiopetuksen opiskeluoikeuden organisaatiohierarkiansa ulkopuolella" in {
      putOpiskeluoikeus(päiväkotiEsiopetus(päiväkotiTouhula, ostopalvelu), headers = authHeaders(MockUsers.jyväskyläTallentaja) ++ jsonContent) {
        verifyResponseStatus(403, KoskiErrorCategory.forbidden.vainVarhaiskasvatuksenJärjestäjä("Operaatio on sallittu vain käyttäjälle joka on luotu varhaiskasvatusta järjestävälle koulutustoimijalle"))
      }
    }
  }

  "Päiväkodin virkailija" - {
    "näkee kaikki omaan organisaatioon luodut opiskeluoikeudet" in {
      val eeroResp = putOpiskeluoikeus(päiväkotiEsiopetus(päiväkotiTouhula, ostopalvelu), henkilö = defaultHenkilö, headers = authHeaders(MockUsers.helsinkiTallentaja) ++ jsonContent) {
        verifyResponseStatusOk()
        readPutOppijaResponse
      }
      val ysiluokkalainenResp = putOpiskeluoikeus(päiväkotiEsiopetus(päiväkotiTouhula, ostopalvelu), henkilö = asUusiOppija(ysiluokkalainen), headers = authHeaders(MockUsers.tornioTallentaja) ++ jsonContent) {
        verifyResponseStatusOk()
        readPutOppijaResponse
      }

      authGet(s"api/opiskeluoikeus/${eeroResp.opiskeluoikeudet.head.oid}", user = MockUsers.touholaKatselija) {
        verifyResponseStatusOk()
      }
      authGet(s"api/opiskeluoikeus/${ysiluokkalainenResp.opiskeluoikeudet.head.oid}", user = MockUsers.touholaKatselija) {
        verifyResponseStatusOk()
      }

      val eeronKoskeenTallennetutOpiskeluoikeudet = oppija(eeroResp.henkilö.oid, MockUsers.touholaKatselija).opiskeluoikeudet.flatMap(_.oid)
      eeronKoskeenTallennetutOpiskeluoikeudet.intersect(eeroResp.opiskeluoikeudet.map(_.oid)) should not be empty
      val ysiluokkalaisenKoskeenTallennetutOpiskeluoikeudet = oppija(ysiluokkalainenResp.henkilö.oid, MockUsers.touholaKatselija).opiskeluoikeudet.flatMap(_.oid)
      ysiluokkalaisenKoskeenTallennetutOpiskeluoikeudet.intersect(ysiluokkalainenResp.opiskeluoikeudet.map(_.oid)) should not be empty
    }
  }
}
