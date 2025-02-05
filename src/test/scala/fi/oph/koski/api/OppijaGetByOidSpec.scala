package fi.oph.koski.api

import fi.oph.koski.{DirtiesFixtures, KoskiHttpSpec}
import fi.oph.koski.documentation.ExampleData.opiskeluoikeusMitätöity
import fi.oph.koski.henkilo.KoskiSpecificMockOppijat
import fi.oph.koski.http.KoskiErrorCategory
import fi.oph.koski.log.AuditLogTester
import fi.oph.koski.schema.AmmatillinenOpiskeluoikeusjakso
import org.scalatest.{FreeSpec, Matchers}

import java.time.LocalDate

class OppijaGetByOidSpec
  extends FreeSpec
    with Matchers
    with KoskiHttpSpec
    with OpiskeluoikeusTestMethods
    with OpiskeluoikeusTestMethodsAmmatillinen
    with DirtiesFixtures {

  "/api/oppija/" - {
    "GET" - {
      "with valid oid" in {
        get("api/oppija/" + KoskiSpecificMockOppijat.eero.oid, headers = authHeaders()) {
          verifyResponseStatusOk()
          AuditLogTester.verifyAuditLogMessage(Map("operation" -> "OPISKELUOIKEUS_KATSOMINEN"))
        }
      }
      "with valid oid, hetuless oppija" in {
        get("api/oppija/" + KoskiSpecificMockOppijat.hetuton.oid, headers = authHeaders()) {
          verifyResponseStatusOk()
          AuditLogTester.verifyAuditLogMessage(Map("operation" -> "OPISKELUOIKEUS_KATSOMINEN"))
        }
      }
      "with invalid oid" in {
        get("api/oppija/blerg", headers = authHeaders()) {
          verifyResponseStatus(400, KoskiErrorCategory.badRequest.queryParam.virheellinenHenkilöOid("Virheellinen oid: blerg. Esimerkki oikeasta muodosta: 1.2.246.562.24.00000000001."))
        }
      }
      "with unknown oid" in {
        get("api/oppija/1.2.246.562.24.90000000001", headers = authHeaders()) {
          verifyResponseStatus(404, KoskiErrorCategory.notFound.oppijaaEiLöydyTaiEiOikeuksia("Oppijaa 1.2.246.562.24.90000000001 ei löydy tai käyttäjällä ei ole oikeuksia tietojen katseluun."))
        }
      }
      "with mitätöity oid" in {
        val oo = createOpiskeluoikeus(KoskiSpecificMockOppijat.eero, defaultOpiskeluoikeus)
        val mitätöity = oo.copy(tila = defaultOpiskeluoikeus.tila.copy(opiskeluoikeusjaksot =
          defaultOpiskeluoikeus.tila.opiskeluoikeusjaksot :+ AmmatillinenOpiskeluoikeusjakso(alku = LocalDate.now, opiskeluoikeusMitätöity)
        ))
        putOpiskeluoikeus(mitätöity, KoskiSpecificMockOppijat.eero, headers = authHeaders() ++ jsonContent) {
          verifyResponseStatusOk()
        }
        get("api/oppija/" + KoskiSpecificMockOppijat.eero.oid, headers = authHeaders()) {
          verifyResponseStatus(404, KoskiErrorCategory.notFound.oppijaaEiLöydyTaiEiOikeuksia(s"Oppijaa ${KoskiSpecificMockOppijat.eero.oid} ei löydy tai käyttäjällä ei ole oikeuksia tietojen katseluun."))
        }
      }
    }
  }
}
