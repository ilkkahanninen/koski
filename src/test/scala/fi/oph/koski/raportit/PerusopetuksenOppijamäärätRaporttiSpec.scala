package fi.oph.koski.raportit

import java.sql.Date.{valueOf => sqlDate}

import fi.oph.koski.KoskiApplicationForTests
import fi.oph.koski.koskiuser.MockUser
import fi.oph.koski.log.AuditLogTester
import fi.oph.koski.organisaatio.MockOrganisaatiot.jyväskylänNormaalikoulu
import fi.oph.koski.raportointikanta.RaportointikantaTestMethods
import org.scalatest.{BeforeAndAfterAll, FreeSpec, Matchers}

class PerusopetuksenOppijamäärätRaporttiSpec extends FreeSpec with Matchers with RaportointikantaTestMethods with BeforeAndAfterAll {
  private val application = KoskiApplicationForTests
  private val raporttiBuilder = PerusopetuksenOppijamäärätRaportti(application.raportointiDatabase.db, application.organisaatioService)
  private lazy val raportti = raporttiBuilder
    .build(Set(jyväskylänNormaalikoulu), sqlDate("2012-01-01"))(session(defaultUser))
    .rows.map(_.asInstanceOf[PerusopetuksenOppijamäärätRaporttiRow])

  override def beforeAll(): Unit = loadRaportointikantaFixtures

  "Perusopetuksen oppijamäärien raportti" - {
    "Raportti voidaan ladata ja lataaminen tuottaa auditlogin" in {
      authGet(s"api/raportit/perusopetuksenoppijamaaratraportti?oppilaitosOid=$jyväskylänNormaalikoulu&paiva=2007-01-01&password=salasana") {
        verifyResponseStatusOk()
        response.headers("Content-Disposition").head should equal(s"""attachment; filename="perusopetuksen_oppijamäärät_raportti-2007-01-01.xlsx"""")
        response.bodyBytes.take(ENCRYPTED_XLSX_PREFIX.length) should equal(ENCRYPTED_XLSX_PREFIX)
        AuditLogTester.verifyAuditLogMessage(
          Map(
            "operation" -> "OPISKELUOIKEUS_RAPORTTI",
            "target" -> Map(
              "hakuEhto" -> s"raportti=perusopetuksenoppijamaaratraportti&oppilaitosOid=$jyväskylänNormaalikoulu&paiva=2007-01-01"
            )
          )
        )
      }
    }

    "Raportin sarakkeet" in {
      val rows = raportti.filter(_.oppilaitosNimi.equals("Jyväskylän normaalikoulu"))
      rows.length should be(5)
      rows.toList should equal(List(
        PerusopetuksenOppijamäärätRaporttiRow(
          oppilaitosNimi = "Jyväskylän normaalikoulu",
          opetuskieli = "suomi",
          vuosiluokka = "6",
          oppilaita = 1,
          vieraskielisiä = 0,
          pidennettyOppivelvollisuusJaVaikeastiVammainen = 0,
          pidennettyOppivelvollisuusJaMuuKuinVaikeimminVammainen = 0,
          virheellisestiSiirretytVaikeastiVammaiset = 0,
          virheellisestiSiirretytMuutKuinVaikeimminVammaiset = 0,
          erityiselläTuella = 0,
          majoitusetu = 0,
          kuljetusetu = 0,
          sisäoppilaitosmainenMajoitus = 0,
          koulukoti = 0,
          joustavaPerusopetus = 0
        ),
        PerusopetuksenOppijamäärätRaporttiRow(
          oppilaitosNimi = "Jyväskylän normaalikoulu",
          opetuskieli = "suomi",
          vuosiluokka = "7",
          oppilaita = 2,
          vieraskielisiä = 1,
          pidennettyOppivelvollisuusJaVaikeastiVammainen = 1,
          pidennettyOppivelvollisuusJaMuuKuinVaikeimminVammainen = 1,
          virheellisestiSiirretytVaikeastiVammaiset = 1,
          virheellisestiSiirretytMuutKuinVaikeimminVammaiset = 1,
          erityiselläTuella = 0,
          majoitusetu = 1,
          kuljetusetu = 1,
          sisäoppilaitosmainenMajoitus = 1,
          koulukoti = 1,
          joustavaPerusopetus = 1
        ),
        PerusopetuksenOppijamäärätRaporttiRow(
          oppilaitosNimi = "Jyväskylän normaalikoulu",
          opetuskieli = "suomi",
          vuosiluokka = "8",
          oppilaita = 1,
          vieraskielisiä = 0,
          pidennettyOppivelvollisuusJaVaikeastiVammainen = 1,
          pidennettyOppivelvollisuusJaMuuKuinVaikeimminVammainen = 0,
          virheellisestiSiirretytVaikeastiVammaiset = 0,
          virheellisestiSiirretytMuutKuinVaikeimminVammaiset = 0,
          erityiselläTuella = 1,
          majoitusetu = 1,
          kuljetusetu = 1,
          sisäoppilaitosmainenMajoitus = 1,
          koulukoti = 1,
          joustavaPerusopetus = 1
        ),
        PerusopetuksenOppijamäärätRaporttiRow(
          oppilaitosNimi = "Jyväskylän normaalikoulu",
          opetuskieli = "suomi",
          vuosiluokka = "9",
          oppilaita = 3,
          vieraskielisiä = 0,
          pidennettyOppivelvollisuusJaVaikeastiVammainen = 0,
          pidennettyOppivelvollisuusJaMuuKuinVaikeimminVammainen = 0,
          virheellisestiSiirretytVaikeastiVammaiset = 0,
          virheellisestiSiirretytMuutKuinVaikeimminVammaiset = 0,
          erityiselläTuella = 0,
          majoitusetu = 0,
          kuljetusetu = 0,
          sisäoppilaitosmainenMajoitus = 0,
          koulukoti = 0,
          joustavaPerusopetus = 0
        ),
        PerusopetuksenOppijamäärätRaporttiRow(
          oppilaitosNimi = "Jyväskylän normaalikoulu",
          opetuskieli = "suomi",
          vuosiluokka = "Kaikki vuosiluokat yhteensä",
          oppilaita = 7,
          vieraskielisiä = 1,
          pidennettyOppivelvollisuusJaVaikeastiVammainen = 2,
          pidennettyOppivelvollisuusJaMuuKuinVaikeimminVammainen = 1,
          virheellisestiSiirretytVaikeastiVammaiset = 1,
          virheellisestiSiirretytMuutKuinVaikeimminVammaiset = 1,
          erityiselläTuella = 1,
          majoitusetu = 2,
          kuljetusetu = 2,
          sisäoppilaitosmainenMajoitus = 2,
          koulukoti = 2,
          joustavaPerusopetus = 2
        )
      ))
    }
  }

  private def session(user: MockUser) = user.toKoskiUser(application.käyttöoikeusRepository)
}
