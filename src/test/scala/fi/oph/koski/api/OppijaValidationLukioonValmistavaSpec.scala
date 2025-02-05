package fi.oph.koski.api

import fi.oph.koski.KoskiHttpSpec
import fi.oph.koski.documentation.ExampleData._
import fi.oph.koski.documentation.ExamplesLukioonValmistavaKoulutus.{lukioonValmistavanKoulutuksenSuoritus, lukioonValmistavanKoulutuksenSuoritus2019, luvaKurssinSuoritus, valtakunnallinenLuvaKurssi}
import fi.oph.koski.documentation.Lukio2019ExampleData.{moduulinSuoritusOppiaineissa, muuModuuliOppiaineissa, numeerinenArviointi, numeerinenLukionOppiaineenArviointi}
import fi.oph.koski.documentation.LukioExampleData.{kurssisuoritus, laajuus, lukionKieli, valtakunnallinenKurssi}
import fi.oph.koski.documentation.{ExamplesLukioonValmistavaKoulutus, Lukio2019ExampleData, LukioExampleData}
import fi.oph.koski.http.KoskiErrorCategory
import fi.oph.koski.schema._

import java.time.LocalDate
import java.time.LocalDate.{of => date}
import scala.reflect.runtime.universe.TypeTag

class OppijaValidationLukioonValmistavaSpec extends TutkinnonPerusteetTest[LukioonValmistavanKoulutuksenOpiskeluoikeus] with KoskiHttpSpec {
  def opiskeluoikeusWithPerusteenDiaarinumero(diaari: Option[String]) = defaultOpiskeluoikeus.copy(suoritukset = List(
    lukioonValmistavanKoulutuksenSuoritus.copy(koulutusmoduuli = lukioonValmistavanKoulutuksenSuoritus.koulutusmoduuli.copy(perusteenDiaarinumero = diaari))
  ))
  def eperusteistaLöytymätönValidiDiaarinumero: String = "33/011/2003"
  override def tag: TypeTag[LukioonValmistavanKoulutuksenOpiskeluoikeus] = implicitly[TypeTag[LukioonValmistavanKoulutuksenOpiskeluoikeus]]
  override def defaultOpiskeluoikeus: LukioonValmistavanKoulutuksenOpiskeluoikeus = ExamplesLukioonValmistavaKoulutus.lukioonValmistavanKoulutuksenOpiskeluoikeus

  "Opintojen rahoitus" - {
    "lasna -tilalta vaaditaan opintojen rahoitus" in {
      putOpiskeluoikeus(defaultOpiskeluoikeus.copy(tila = LukionOpiskeluoikeudenTila(List(LukionOpiskeluoikeusjakso(longTimeAgo, opiskeluoikeusLäsnä))))) {
        verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.tila.tilaltaPuuttuuRahoitusmuoto("Opiskeluoikeuden tilalta lasna puuttuu rahoitusmuoto"))
      }
    }
    "valmistunut -tila vaaditaan opintojen rahoitus" in {
      val tila = LukionOpiskeluoikeudenTila(List(
        LukionOpiskeluoikeusjakso(longTimeAgo, opiskeluoikeusLäsnä, Some(valtionosuusRahoitteinen)),
        LukionOpiskeluoikeusjakso(date(2016, 6, 4), opiskeluoikeusValmistunut))
      )
      putOpiskeluoikeus(defaultOpiskeluoikeus.copy(tila = tila)) {
        verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.tila.tilaltaPuuttuuRahoitusmuoto("Opiskeluoikeuden tilalta valmistunut puuttuu rahoitusmuoto"))
      }
    }
  }

  "Opintojen osasuoritukset" - {
    "osasuorituksissa ei voi olla samaan aikaan lukion 2015 ja lukion 2019 opsien mukaisia lukion oppiaineiden suorituksia" in {
      val opiskeluoikeus = ExamplesLukioonValmistavaKoulutus.lukioonValmistavanKoulutuksenOpiskeluoikeus2019.copy(
        suoritukset = List(lukioonValmistavanKoulutuksenSuoritus2019.copy(
          osasuoritukset = Some(List(
            LukionOppiaineenOpintojenSuoritusLukioonValmistavassaKoulutuksessa(
              lukionKieli("A1", "EN"),
              arviointi = LukioExampleData.arviointi("S"),
              osasuoritukset = Some(List(
                kurssisuoritus(valtakunnallinenKurssi("ENA1")).copy(arviointi = LukioExampleData.numeerinenArviointi(8))
              ))
            ),
            LukionOppiaineenOpintojenSuoritusLukioonValmistavassaKoulutuksessa2019(
              koulutusmoduuli = Lukio2019ExampleData.lukionUskonto(Some("MU")),
              arviointi = numeerinenLukionOppiaineenArviointi(9),
              osasuoritukset = Some(List(
                moduulinSuoritusOppiaineissa(muuModuuliOppiaineissa("UE1").copy(laajuus = Lukio2019ExampleData.laajuus(1.5))).copy(arviointi = numeerinenArviointi(4))
              ))
            )
          ))
        ))
      )

      putOpiskeluoikeus(opiskeluoikeus) {
        verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.rakenne.lukioonValmistavassaEriLukioOpsienOsasuorituksia())
      }
    }

    "sallitaan vain luva2019-koodiston mukaisia osasuorituksia 1.8.2021 ja jälkeen" - {
      "estetään luva2015-opsin mukainen kurssisuoritus" in {
        val opiskeluoikeus = ExamplesLukioonValmistavaKoulutus.lukioonValmistavanKoulutuksenOpiskeluoikeus2019.copy(
          suoritukset = List(lukioonValmistavanKoulutuksenSuoritus2019.copy(
            osasuoritukset = Some(List(
              LukioonValmistavanKoulutuksenOppiaineenSuoritus(
                LukioonValmistavaÄidinkieliJaKirjallisuus(Koodistokoodiviite("LVAIK", "oppiaineetluva"), kieli = Koodistokoodiviite(koodiarvo = "AI7", koodistoUri = "oppiaineaidinkielijakirjallisuus")),
                arviointi = arviointiPäivämäärällä("S", LocalDate.of(2021, 8, 1)),
                osasuoritukset = Some(List(
                  ExamplesLukioonValmistavaKoulutus.luvaKurssinSuoritus(ExamplesLukioonValmistavaKoulutus.valtakunnallinenLuvaKurssi("LVS1").copy(
                    laajuus = Some(laajuus(2.0f)))).copy(
                    alkamispäivä = Some(LocalDate.of(2021, 8, 1)),
                    arviointi = LukioExampleData.sanallinenArviointi("S", None, LocalDate.of(2021, 8, 1))
                  )
                ))
              )
            ))
          ))
        )

        putOpiskeluoikeus(opiskeluoikeus) {
          verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.rakenne.lukioonValmistavassaVanhanOpsinKurssiSuorituksia("Lukioon valmistavan koulutuksen kurssilla lukioonvalmistavankoulutuksenkurssit2015/LVS1 on vanhan opetussuunniteleman mukainen koodi. 1.8.2021 jälkeen alkaneiden kurssien tulee käyttää vuoden 2021 opetussuunnitelmaa."))
        }
      }
      "sallitaan luva2019-opsin mukainen kurssisuoritus" in {
        val opiskeluoikeus = ExamplesLukioonValmistavaKoulutus.lukioonValmistavanKoulutuksenOpiskeluoikeus2019.copy(
          suoritukset = List(lukioonValmistavanKoulutuksenSuoritus2019.copy(
            osasuoritukset = Some(List(
              LukioonValmistavanKoulutuksenOppiaineenSuoritus(
                LukioonValmistavaÄidinkieliJaKirjallisuus(Koodistokoodiviite("LVAIK", "oppiaineetluva"), kieli = Koodistokoodiviite(koodiarvo = "AI7", koodistoUri = "oppiaineaidinkielijakirjallisuus")),
                arviointi = arviointiPäivämäärällä("S", LocalDate.of(2021, 8, 1)),
                osasuoritukset = Some(List(
                  ExamplesLukioonValmistavaKoulutus.luvaKurssinSuoritus(ExamplesLukioonValmistavaKoulutus.valtakunnallinenLuvaKurssi2019("LVS1").copy(
                    laajuus = Some(laajuus(2.0f)))).copy(
                    alkamispäivä = Some(LocalDate.of(2021, 8, 1)),
                    arviointi = LukioExampleData.sanallinenArviointi("S", None, LocalDate.of(2021, 8, 1))
                  )
                ))
              )
            ))
          ))
        )

        putOpiskeluoikeus(opiskeluoikeus) {
          verifyResponseStatusOk()
        }
      }
    }
  }

  def arviointiPäivämäärällä(arvosana: String, päivä: LocalDate): Some[List[LukionOppiaineenArviointi]] = {
    Some(List(LukionOppiaineenArviointi(arvosana, Some(päivä))))
  }
}
