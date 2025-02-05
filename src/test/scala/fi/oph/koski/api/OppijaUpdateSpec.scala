package fi.oph.koski.api

import fi.oph.koski.KoskiHttpSpec
import fi.oph.koski.documentation.AmmatillinenExampleData._
import fi.oph.koski.documentation.AmmatillinenOldExamples.muunAmmatillisenTutkinnonOsanSuoritus
import fi.oph.koski.documentation.ExampleData.{jyväskylä, longTimeAgo, opiskeluoikeusLäsnä, valtionosuusRahoitteinen}
import fi.oph.koski.documentation.ExamplesAikuistenPerusopetus.{aikuistenPerusopetukseOppimääränSuoritus, aikuistenPerusopetus2017, oppiaineidenSuoritukset2017}
import fi.oph.koski.documentation.PerusopetusExampleData.perusopetuksenOppimääränSuoritus
import fi.oph.koski.documentation.YleissivistavakoulutusExampleData.jyväskylänNormaalikoulu
import fi.oph.koski.documentation._
import fi.oph.koski.henkilo.KoskiSpecificMockOppijat
import fi.oph.koski.henkilo.KoskiSpecificMockOppijat.koululainen
import fi.oph.koski.http.{ErrorMatcher, KoskiErrorCategory}
import fi.oph.koski.json.JsonSerializer
import fi.oph.koski.koskiuser.MockUsers.{helsinginKaupunkiPalvelukäyttäjä, helsinkiTallentaja, kalle, paakayttaja}
import fi.oph.koski.koskiuser.{MockUsers, UserWithPassword}
import fi.oph.koski.oppija.HenkilönOpiskeluoikeusVersiot
import fi.oph.koski.organisaatio.{MockOrganisaatioRepository, MockOrganisaatiot}
import fi.oph.koski.schema._
import org.scalatest.FreeSpec

import java.time.LocalDate.{of => date}
import java.time.{LocalDate, LocalDateTime, ZoneId, ZonedDateTime}

class OppijaUpdateSpec extends FreeSpec with KoskiHttpSpec with OpiskeluoikeusTestMethodsAmmatillinen {
  val oppija = KoskiSpecificMockOppijat.tyhjä

  "Opiskeluoikeuden lisääminen" - {
    "Palauttaa oidin ja versiot" in {
      resetFixtures
      putOppija(Oppija(oppija, List(defaultOpiskeluoikeus))) {
        verifyResponseStatusOk()
        val result = JsonSerializer.parse[HenkilönOpiskeluoikeusVersiot](response.body)
        result.henkilö.oid should startWith("1.2.246.562.24.00000000")
        result.opiskeluoikeudet.map(_.versionumero) should equal(List(1))
      }
    }
    "Puuttuvien tietojen täyttäminen" - {
      "Oppilaitoksen tiedot" - {
        "Ilman nimeä -> haetaan nimi" in {
          val opiskeluoikeus = createOpiskeluoikeus(oppija, defaultOpiskeluoikeus)
          opiskeluoikeus.getOppilaitos.nimi.get.get("fi") should equal("Stadin ammatti- ja aikuisopisto")
          opiskeluoikeus.getOppilaitos.oppilaitosnumero.get.koodiarvo should equal("10105")
        }
        "Väärällä nimellä -> korvataan nimi" in {
          val opiskeluoikeus = createOpiskeluoikeus(oppija, defaultOpiskeluoikeus.copy(oppilaitos = Some(Oppilaitos(MockOrganisaatiot.stadinAmmattiopisto, nimi = Some(LocalizedString.finnish("Läppäkoulu"))))))
          opiskeluoikeus.getOppilaitos.nimi.get.get("fi") should equal("Stadin ammatti- ja aikuisopisto")
        }

        "Oppilaitos puuttuu" - {
          "Suoritukselta löytyy toimipiste -> Täytetään oppilaitos" in {
            val opiskeluoikeus = createOpiskeluoikeus(oppija, defaultOpiskeluoikeus.copy(oppilaitos = None))
            opiskeluoikeus.getOppilaitos.oid should equal(MockOrganisaatiot.stadinAmmattiopisto)
          }
          "Suorituksilta löytyy toimipisteitä, joilla sama oppilaitos -> Täytetään oppilaitos" in {
            val opiskeluoikeus = createOpiskeluoikeus(oppija, defaultOpiskeluoikeus.copy(
              oppilaitos = None,
              suoritukset = List(autoalanPerustutkinnonSuoritus(stadinToimipiste), autoalanPerustutkinnonSuoritus(stadinAmmattiopisto))
            ))
            opiskeluoikeus.getOppilaitos.oid should equal(MockOrganisaatiot.stadinAmmattiopisto)
          }
          "Suorituksilta löytyy toimipisteet, joilla eri oppilaitos -> FAIL" in {
            putOpiskeluoikeus(defaultOpiskeluoikeus.copy(
              oppilaitos = None,
              suoritukset = List(autoalanPerustutkinnonSuoritus(stadinToimipiste), autoalanPerustutkinnonSuoritus(OidOrganisaatio(MockOrganisaatiot.omnia)))
            )) {
              verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.organisaatio.oppilaitosPuuttuu("Opiskeluoikeudesta puuttuu oppilaitos, eikä sitä voi yksiselitteisesti päätellä annetuista toimipisteistä."))
            }
          }
        }
        "Oppisopimustoimisto hyväksytään opiskeluoikeuden oppilaitokseksi" in {
          putOpiskeluoikeus(makeOpiskeluoikeus(alkamispäivä = longTimeAgo, toimpiste = stadinOppisopimuskeskus).copy(oppilaitos = None), headers = authHeaders(helsinkiTallentaja) ++ jsonContent) {
            verifyResponseStatusOk()
          }
        }
      }
      "Koodistojen tiedot" - {
        "Ilman nimeä -> haetaan nimi" in {
          val opiskeluoikeus = createOpiskeluoikeus(oppija, defaultOpiskeluoikeus)
          val suoritus = opiskeluoikeus.suoritukset(0).asInstanceOf[AmmatillisenTutkinnonSuoritus]
          suoritus.koulutusmoduuli.tunniste.nimi.get.get("fi") should equal("Autoalan perustutkinto")
          suoritus.koulutusmoduuli.tunniste.nimi.get.get("sv") should equal("Grundexamen inom bilbranschen")
        }
        "Väärällä nimellä -> korvataan nimi" in {
          val opiskeluoikeus = createOpiskeluoikeus(oppija, defaultOpiskeluoikeus.copy(suoritukset = List(autoalanPerustutkinnonSuoritus().copy(koulutusmoduuli = autoalanPerustutkinnonSuoritus().koulutusmoduuli.copy(tunniste = Koodistokoodiviite(koodiarvo = "351301", nimi=Some(LocalizedString.finnish("Läppätutkinto")), koodistoUri = "koulutus"))))))

          opiskeluoikeus.suoritukset(0).asInstanceOf[AmmatillisenTutkinnonSuoritus].koulutusmoduuli.tunniste.nimi.get.get("fi") should equal("Autoalan perustutkinto")
        }
      }
      "Koulutustyyppi" - {
        "nuorten perusopetus" in {
          resetFixtures
          val oo = PerusopetuksenOpiskeluoikeus(
            oppilaitos = Some(jyväskylänNormaalikoulu),
            suoritukset = List(perusopetuksenOppimääränSuoritus),
            tila = NuortenPerusopetuksenOpiskeluoikeudenTila(List(NuortenPerusopetuksenOpiskeluoikeusjakso(longTimeAgo, opiskeluoikeusLäsnä)))
          )
          createOpiskeluoikeus(oppija, oo).suoritukset.head.koulutusmoduuli.asInstanceOf[Koulutus].koulutustyyppi.get.koodiarvo should equal("16")
        }

        "aikuisten perusopetus" in {
          resetFixtures
          val oo = AikuistenPerusopetuksenOpiskeluoikeus(
            oppilaitos = Some(jyväskylänNormaalikoulu),
            suoritukset = List(aikuistenPerusopetukseOppimääränSuoritus(aikuistenPerusopetus2017, oppiaineidenSuoritukset2017)),
            tila = AikuistenPerusopetuksenOpiskeluoikeudenTila(List(AikuistenPerusopetuksenOpiskeluoikeusjakso(longTimeAgo, opiskeluoikeusLäsnä, Some(valtionosuusRahoitteinen))))
          )
          createOpiskeluoikeus(oppija, oo).suoritukset.head.koulutusmoduuli.asInstanceOf[Koulutus].koulutustyyppi.get.koodiarvo should equal("17")
        }

        "ammatillinen" in {
          resetFixtures
          val oo = createOpiskeluoikeus(oppija, defaultOpiskeluoikeus.copy(suoritukset = List(autoalanPerustutkinnonSuoritus())))
          oo.suoritukset.head.koulutusmoduuli.asInstanceOf[Koulutus].koulutustyyppi.get.koodiarvo should equal("1")
        }
      }

      "Koulutustoimijan tiedot" in {
        resetFixtures
        val opiskeluoikeus = createOpiskeluoikeus(oppija, defaultOpiskeluoikeus)
        opiskeluoikeus.koulutustoimija.map(_.oid) should equal(Some("1.2.246.562.10.346830761110"))
      }
    }
    "Organisaation nimi on muuttunut" - {

      val tutkinto: AmmatillisenTutkinnonSuoritus = defaultOpiskeluoikeus.suoritukset.head.asInstanceOf[AmmatillisenTutkinnonSuoritus]
      val tutkintoOsasuorituksilla = tutkinto.copy(osasuoritukset = Some(List(tutkinnonOsanSuoritus("100031", "Moottorin ja voimansiirron huolto ja korjaus", ammatillisetTutkinnonOsat, k3, 40).copy(vahvistus = None))))
      val oo = defaultOpiskeluoikeus.copy(suoritukset = List(tutkintoOsasuorituksilla))

      def nimi(org: OrganisaatioWithOid) = org.nimi.get.get("fi")
      def tutkinnonSuoritus(opiskeluoikeus: Opiskeluoikeus): AmmatillisenTutkinnonSuoritus = opiskeluoikeus.suoritukset.head.asInstanceOf[AmmatillisenTutkinnonSuoritus]
      def osasuoritus(opiskeluoikeus: Opiskeluoikeus): AmmatillisenTutkinnonOsanSuoritus = tutkinnonSuoritus(opiskeluoikeus).osasuoritukset.toList.flatten.head

      "Käytetään uusinta nimeä, jos opiskeluoikeus ei ole päättynyt" in {
        val opiskeluoikeus = createOpiskeluoikeus(oppija, oo)
        nimi(opiskeluoikeus.getOppilaitos) should equal("Stadin ammatti- ja aikuisopisto")
        nimi(opiskeluoikeus.koulutustoimija.get) should equal("Helsingin kaupunki")
        nimi(tutkinnonSuoritus(opiskeluoikeus).toimipiste) should equal("Stadin ammatti- ja aikuisopisto, Lehtikuusentien toimipaikka")
        nimi(osasuoritus(opiskeluoikeus).toimipiste.get) should equal("Stadin ammatti- ja aikuisopisto, Lehtikuusentien toimipaikka")
      }

      "Käytetään nimeä joka organisaatiolla oli opiskeluoikeuden päättymisen aikaan" in {
        val opiskeluoikeus = createOpiskeluoikeus(oppija, päättymispäivällä(oo, LocalDate.of(2010, 10, 10)))
        nimi(opiskeluoikeus.getOppilaitos) should equal("Stadin ammatti- ja aikuisopisto -vanha")
        nimi(opiskeluoikeus.koulutustoimija.get) should equal("Helsingin kaupunki -vanha")
        nimi(tutkinnonSuoritus(opiskeluoikeus).toimipiste) should equal("Stadin ammatti- ja aikuisopisto, Lehtikuusentien toimipaikka -vanha")
        nimi(osasuoritus(opiskeluoikeus).toimipiste.get) should equal("Stadin ammatti- ja aikuisopisto, Lehtikuusentien toimipaikka -vanha")
      }
    }
  }

  "Opiskeluoikeuden muokkaaminen" - {

    "Käytettäessä opiskeluoikeus-oid:ia" - {
      "Muokkaa olemassaolevaa opiskeluoikeutta" in {
        resetFixtures
        import fi.oph.koski.util.DateOrdering._
        val d: LocalDate = date(2020, 1, 1)
        var aikaleima: Option[LocalDateTime] = None
        verifyChange(change = { existing: AmmatillinenOpiskeluoikeus =>
          aikaleima = existing.aikaleima
          existing.copy(arvioituPäättymispäivä = Some(d))
        }) {
          verifyResponseStatusOk()
          val result: KoskeenTallennettavaOpiskeluoikeus = lastOpiskeluoikeusByHetu(oppija)
          result.arvioituPäättymispäivä should equal(Some(d))
          result.versionumero should equal(Some(2))
          result.aikaleima.get should be > aikaleima.get
        }
      }

      "Aikaleima on Suomen aikavyöhykkeessä" in {
        resetFixtures
        verifyChange(change = { existing: AmmatillinenOpiskeluoikeus =>
          existing.copy(arvioituPäättymispäivä = Some(date(2020, 1, 1)))
        }) {
          verifyResponseStatusOk()
          val result: KoskeenTallennettavaOpiskeluoikeus = lastOpiskeluoikeusByHetu(oppija)
          val aikaleima = result.aikaleima.get
          val helsinkiTime = ZonedDateTime.now(ZoneId.of("Europe/Helsinki"))
          Math.abs(aikaleima.getHour - helsinkiTime.getHour) should be < 2
        }
      }

      "Sallii oppilaitoksen vaihtamisen" in {
        resetFixtures
        verifyChange(change = {existing: AmmatillinenOpiskeluoikeus => existing.copy(koulutustoimija = None, oppilaitos = Some(Oppilaitos(MockOrganisaatiot.omnia)))}) {
          verifyResponseStatusOk()
          val result: KoskeenTallennettavaOpiskeluoikeus = lastOpiskeluoikeusByHetu(oppija)
          result.versionumero should equal(Some(2))
        }
      }

      "Estää tyypin vaihtamisen" in {
        verifyChange(change = {existing: AmmatillinenOpiskeluoikeus => TestMethodsLukio.lukionOpiskeluoikeus.copy(oid = existing.oid, oppilaitos = existing.oppilaitos)}) {
          verifyResponseStatus(403, KoskiErrorCategory.forbidden.kiellettyMuutos("Opiskeluoikeuden tyyppiä ei voi vaihtaa. Vanha tyyppi ammatillinenkoulutus. Uusi tyyppi lukiokoulutus."))
        }
      }

      "Mahdollistaa lähdejärjestelmä-id:n vaihtamisen (case: oppilaitos vaihtaa tietojärjestelmää)" in {
        val original: AmmatillinenOpiskeluoikeus = defaultOpiskeluoikeus.copy(lähdejärjestelmänId = Some(winnovaLähdejärjestelmäId))

        verifyChange(original = original, user = helsinginKaupunkiPalvelukäyttäjä, change = { existing: AmmatillinenOpiskeluoikeus => existing.copy(lähdejärjestelmänId = Some(primusLähdejärjestelmäId)) }) {
          verifyResponseStatusOk()
          val result: KoskeenTallennettavaOpiskeluoikeus = lastOpiskeluoikeusByHetu(oppija)
          result.lähdejärjestelmänId.map(_.lähdejärjestelmä.koodiarvo) should equal(Some(primusLähdejärjestelmäId.lähdejärjestelmä.koodiarvo))
        }
      }

      "Estää opiskeluoikeuden siirtymisen eri henkilölle" in {
        val original = createOpiskeluoikeus(KoskiSpecificMockOppijat.eero, defaultOpiskeluoikeus)

        putOpiskeluoikeus(original.copy(arvioituPäättymispäivä = Some(LocalDate.now())), oppija) {
          verifyResponseStatus(403, ErrorMatcher.regex(KoskiErrorCategory.forbidden.oppijaOidinMuutos, "Oppijan oid.*ei löydy opiskeluoikeuden oppijan oideista.*".r))
        }
      }

      "Sallii opiskeluoikeuden päivittämisen Master-henkilön oidilla" in {
        createOpiskeluoikeus(KoskiSpecificMockOppijat.master, defaultOpiskeluoikeus)
        val original = createOpiskeluoikeus(KoskiSpecificMockOppijat.slave.henkilö, defaultOpiskeluoikeus)

        putOpiskeluoikeus(original.copy(arvioituPäättymispäivä = Some(LocalDate.now())), KoskiSpecificMockOppijat.master) {
          verifyResponseStatusOk()
        }
      }

      "Opiskeluoikeuden luominen slave-henkilön tiedoilla" in {
        createOrUpdate(KoskiSpecificMockOppijat.slaveMasterEiKoskessa.henkilö, defaultOpiskeluoikeus)
      }
    }

    "Käytettäessä lähdejärjestelmä-id:tä" - {
      val original: AmmatillinenOpiskeluoikeus = defaultOpiskeluoikeus.copy(lähdejärjestelmänId = Some(winnovaLähdejärjestelmäId))

      "Muokkaa olemassaolevaa opiskeluoikeutta, kun lähdejärjestelmä-id on sama" in {
        resetFixtures
        val d: LocalDate = date(2020, 1, 1)
        verifyChange(original = original, user = helsinginKaupunkiPalvelukäyttäjä, change = { existing: AmmatillinenOpiskeluoikeus => existing.copy(oid = None, versionumero = None, arvioituPäättymispäivä = Some(d))}) {
          verifyResponseStatusOk()
          val result: KoskeenTallennettavaOpiskeluoikeus = lastOpiskeluoikeusByHetu(oppija)
          result.arvioituPäättymispäivä should equal(Some(d))
          result.versionumero should equal(Some(2))
        }
      }

      "Sallii oppilaitoksen vaihtamisen" in {
        resetFixtures
        verifyChange(original = original, user = paakayttaja, change = {existing: AmmatillinenOpiskeluoikeus => existing.copy(oid = None, versionumero = None, koulutustoimija = None, oppilaitos = Some(Oppilaitos(MockOrganisaatiot.omnia)))}) {
          verifyResponseStatusOk()
          val result: KoskeenTallennettavaOpiskeluoikeus = lastOpiskeluoikeusByHetu(oppija)
          result.versionumero should equal(Some(2))
        }
      }

      "Estää tyypin vaihtamisen" in {
        verifyChange(original = original, user = paakayttaja, change = {existing: AmmatillinenOpiskeluoikeus => TestMethodsLukio.lukionOpiskeluoikeus.copy(lähdejärjestelmänId = Some(winnovaLähdejärjestelmäId), oppilaitos = existing.oppilaitos)}) {
          verifyResponseStatus(403, KoskiErrorCategory.forbidden.kiellettyMuutos("Opiskeluoikeuden tyyppiä ei voi vaihtaa. Vanha tyyppi ammatillinenkoulutus. Uusi tyyppi lukiokoulutus."))
        }
      }

      "Mahdollistaa toisen opiskeluoikeuden luonnin samalla tyypillä ja oppilaitoksella, kunhan lähdejärjestelmä-id on eri" in {
        resetFixtures
        val lähdejärjestelmänId2 = LähdejärjestelmäId(Some("123452"), AmmatillinenExampleData.lähdeWinnova)
        verifyChange(original = original, user = helsinginKaupunkiPalvelukäyttäjä, change = { existing: AmmatillinenOpiskeluoikeus => existing.copy(oid = None, versionumero = None, lähdejärjestelmänId = Some(lähdejärjestelmänId2))}) {
          verifyResponseStatusOk()
          val result: KoskeenTallennettavaOpiskeluoikeus = lastOpiskeluoikeusByHetu(oppija)
          result.lähdejärjestelmänId.map(_.id) should equal(Some(lähdejärjestelmänId2.id))
          result.versionumero should equal(Some(1))
        }
      }

      "Estää opiskeluoikeuden siirtymisen eri henkilölle" in {
        resetFixtures
        val lähdejärjestelmänId2 = LähdejärjestelmäId(Some("123452"), AmmatillinenExampleData.lähdeWinnova)
        createOpiskeluoikeus(koululainen, original, user = helsinginKaupunkiPalvelukäyttäjä)
        val opiskeluoikeus = createOpiskeluoikeus(oppija, defaultOpiskeluoikeus)

        createOrUpdate(koululainen, opiskeluoikeus.copy(lähdejärjestelmänId = Some(winnovaLähdejärjestelmäId)), {
          verifyResponseStatus(403, ErrorMatcher.regex(KoskiErrorCategory.forbidden.oppijaOidinMuutos, "Oppijan oid.*ei löydy opiskeluoikeuden oppijan oideista.*".r))
        }, helsinginKaupunkiPalvelukäyttäjä)
      }
    }

    "Käytettäessä vain oppilaitoksen tietoa ja opiskeluoikeuden tyyppiä" - {
      "Muokkaa olemassaolevaa opiskeluoikeutta, jos sama oppilaitos ja opiskeluoikeustyyppi (estää siis useamman luonnin)" in {
        resetFixtures
        val d: LocalDate = date(2020, 1, 1)
        verifyChange(change = {existing: AmmatillinenOpiskeluoikeus => existing.copy(oid = None, arvioituPäättymispäivä = Some(d))}) {
          verifyResponseStatusOk()
          val result: KoskeenTallennettavaOpiskeluoikeus = lastOpiskeluoikeusByHetu(oppija)
          result.arvioituPäättymispäivä should equal(Some(d))
          result.versionumero should equal(Some(2))
        }
      }

      "Jos olemassa olevassa opiskeluoikeudessa on lähdejärjestelmä-id, ei päivitetä" in {
        resetFixtures
        val lähdejärjestelmänId = LähdejärjestelmäId(Some("12345"), AmmatillinenExampleData.lähdeWinnova)
        verifyChange(original = defaultOpiskeluoikeus.copy(lähdejärjestelmänId = Some(lähdejärjestelmänId)), user = helsinginKaupunkiPalvelukäyttäjä, user2 = Some(kalle), change = { existing: AmmatillinenOpiskeluoikeus => existing.copy(oid = None, lähdejärjestelmänId = None)}) {
          verifyResponseStatusOk()
          val result: KoskeenTallennettavaOpiskeluoikeus = lastOpiskeluoikeusByHetu(oppija)
          result.versionumero should equal(Some(1))
        }
      }

      "Jos oppilaitos vaihtuu, tekee uuden opiskeluoikeuden" in {
        resetFixtures
        verifyChange(change = {existing: AmmatillinenOpiskeluoikeus => existing.copy(oid = None, versionumero = None, koulutustoimija = None, oppilaitos = Some(Oppilaitos(MockOrganisaatiot.omnia)))}) {
          verifyResponseStatusOk()
          val result: KoskeenTallennettavaOpiskeluoikeus = lastOpiskeluoikeusByHetu(oppija)
          result.getOppilaitos.oid should equal(MockOrganisaatiot.omnia)
          result.versionumero should equal(Some(1))
        }
      }

      "Jos tyyppi vaihtuu, tekee uuden opiskeluoikeuden" in {
        resetFixtures
        verifyChange(change = {existing: AmmatillinenOpiskeluoikeus => TestMethodsLukio.lukionOpiskeluoikeus.copy(oppilaitos = existing.oppilaitos)}) {
          verifyResponseStatusOk()
          val result: KoskeenTallennettavaOpiskeluoikeus = lastOpiskeluoikeusByHetu(oppija)
          result.tyyppi.koodiarvo should equal(TestMethodsLukio.lukionOpiskeluoikeus.tyyppi.koodiarvo)
          result.versionumero should equal(Some(1))
        }
      }
    }

    "Jos valmis päätason suoritus on poistunut" - {
      "Ammatillisessa aiemmin tallennettua suoritusta ei säilytetä" in {
        resetFixtures
        val vanhaValmisSuoritus = valmis(ammatillinenTutkintoSuoritus(autoalanPerustutkinto)).copy(osasuoritukset = Some(List(muunAmmatillisenTutkinnonOsanSuoritus)))
        val vanhaKeskeneräinenSuoritus = ammatillinenTutkintoSuoritus(puutarhuri)
        val uusiSuoritus = ammatillinenTutkintoSuoritus(parturikampaaja)
        val oo = defaultOpiskeluoikeus.copy(suoritukset = List(vanhaValmisSuoritus, vanhaKeskeneräinenSuoritus))
        def poistaSuoritukset(oo: AmmatillinenOpiskeluoikeus) = oo.copy(suoritukset = List(uusiSuoritus))
        verifyChange(original = oo, change = poistaSuoritukset) {
          verifyResponseStatusOk()
          val result: AmmatillinenOpiskeluoikeus = lastOpiskeluoikeusByHetu(oppija).asInstanceOf[AmmatillinenOpiskeluoikeus]
          result.suoritukset.map(_.koulutusmoduuli.tunniste.koodiarvo) should equal(List(uusiSuoritus.koulutusmoduuli.tunniste.koodiarvo))
        }
      }
      "lukionoppiaineenoppimaara:n suorituksia ei säilytetä" in {
        resetFixtures
        val vanhaValmisSuoritus = LukioExampleData.lukionOppiaineenOppimääränSuoritusYhteiskuntaoppi
        val vanhaKeskeneräinenSuoritus = LukioExampleData.lukionOppiaineenOppimääränSuoritusFilosofia.copy(vahvistus = None)
        val uusiSuoritus = ExamplesLukio.aineopiskelija.suoritukset.head
        val oo = ExamplesLukio.aineOpiskelijaKesken.copy(suoritukset = List(vanhaValmisSuoritus, vanhaKeskeneräinenSuoritus))
        def poistaSuoritukset(oo: LukionOpiskeluoikeus) = oo.copy(suoritukset = List(uusiSuoritus))
        verifyChange(original = oo, change = poistaSuoritukset) {
          verifyResponseStatusOk()
          val result = lastOpiskeluoikeusByHetu(oppija)
          result.suoritukset.map(_.koulutusmoduuli.tunniste.koodiarvo) should equal(List(uusiSuoritus.koulutusmoduuli.tunniste.koodiarvo))
        }
      }
      "perusopetuksenoppiaineenoppimaara:n suorituksia ei säilytetä" in {
        resetFixtures
        val diaarinumero = Some("OPH-1280-2017")
        val vanhaValmisSuoritus = ExamplesAikuistenPerusopetus.oppiaineenOppimääränSuoritus(ExamplesAikuistenPerusopetus.aikuistenOppiaine("YH").copy(perusteenDiaarinumero = diaarinumero))
        val vanhaKeskeneräinenSuoritus = ExamplesAikuistenPerusopetus.oppiaineenOppimääränSuoritus(ExamplesAikuistenPerusopetus.aikuistenOppiaine("FI").copy(perusteenDiaarinumero = diaarinumero)).copy(vahvistus = None)
        val uusiSuoritus = ExamplesAikuistenPerusopetus.oppiaineenOppimääränSuoritus(ExamplesAikuistenPerusopetus.aikuistenOppiaine("KE").copy(perusteenDiaarinumero = diaarinumero))
        val oo = ExamplesAikuistenPerusopetus.oppiaineenOppimääräOpiskeluoikeus.copy(suoritukset = List(vanhaValmisSuoritus, vanhaKeskeneräinenSuoritus), tila = AikuistenPerusopetuksenOpiskeluoikeudenTila(List(AikuistenPerusopetuksenOpiskeluoikeusjakso(date(2008, 1, 1), opiskeluoikeusLäsnä, Some(valtionosuusRahoitteinen)))))
        def poistaSuoritukset(oo: AikuistenPerusopetuksenOpiskeluoikeus) = oo.copy(suoritukset = List(uusiSuoritus))
        verifyChange(original = oo, change = poistaSuoritukset) {
          verifyResponseStatusOk()
          val result = lastOpiskeluoikeusByHetu(oppija)
          result.suoritukset.map(_.koulutusmoduuli.tunniste.koodiarvo) should equal(List(uusiSuoritus.koulutusmoduuli.tunniste.koodiarvo))
        }
      }
      "nuortenperusopetuksenoppiaineenoppimaara:n suorituksia ei säilytetä" in {
        resetFixtures
        val vanhaValmisSuoritus = PerusopetusExampleData.nuortenPerusOpetuksenOppiaineenOppimääränSuoritus("KU")
        val vanhaKeskeneräinenSuoritus = PerusopetusExampleData.nuortenPerusOpetuksenOppiaineenOppimääränSuoritus("LI")
        val uusiSuoritus = PerusopetusExampleData.nuortenPerusOpetuksenOppiaineenOppimääränSuoritus("FI")
        val oo = PerusopetusExampleData.opiskeluoikeus(suoritukset = List(vanhaValmisSuoritus, vanhaKeskeneräinenSuoritus), päättymispäivä = None)
        def poistaSuoritukset(oo: PerusopetuksenOpiskeluoikeus) = oo.copy(suoritukset = List(uusiSuoritus))
        verifyChange(original = oo, change = poistaSuoritukset) {
          verifyResponseStatusOk()
          val result = lastOpiskeluoikeusByHetu(oppija)
          result.suoritukset.map(_.koulutusmoduuli.tunniste.koodiarvo) should equal(List(uusiSuoritus.koulutusmoduuli.tunniste.koodiarvo))
        }
      }
      "Muuten aiemmin tallennettu suoritus säilytetään" in {
        resetFixtures
        val vanhaValmisSuoritus = PerusopetusExampleData.seitsemännenLuokanSuoritus
        val vanhaKeskenSuoritus = PerusopetusExampleData.kahdeksannenLuokanSuoritus.copy(vahvistus = None)
        val uusiSuoritus = PerusopetusExampleData.yhdeksännenLuokanSuoritus.copy(vahvistus = None)
        val oo = PerusopetusExampleData.opiskeluoikeus(suoritukset = List(vanhaValmisSuoritus, vanhaKeskenSuoritus), päättymispäivä = None).copy(tila = NuortenPerusopetuksenOpiskeluoikeudenTila(List(NuortenPerusopetuksenOpiskeluoikeusjakso(longTimeAgo, opiskeluoikeusLäsnä))))
        def poistaSuoritukset(oo: PerusopetuksenOpiskeluoikeus) = oo.copy(suoritukset = List(uusiSuoritus))
        verifyChange(original = oo, change = poistaSuoritukset) {
          verifyResponseStatusOk()
          val result: PerusopetuksenOpiskeluoikeus = lastOpiskeluoikeusByHetu(oppija).asInstanceOf[PerusopetuksenOpiskeluoikeus]
          result.suoritukset.map(_.koulutusmoduuli.tunniste.koodiarvo) should equal(List(vanhaValmisSuoritus.koulutusmoduuli.tunniste.koodiarvo, uusiSuoritus.koulutusmoduuli.tunniste.koodiarvo))
        }
      }
    }

    "Organisaation muutoshistoria" - {
      lazy val uusiOrganisaatioHistoria = OpiskeluoikeudenOrganisaatiohistoria(
        LocalDate.now(),
        MockOrganisaatioRepository.getOrganisaatioHierarkia(MockOrganisaatiot.stadinAmmattiopisto).flatMap(_.toOppilaitos),
        MockOrganisaatioRepository.getOrganisaatioHierarkia(MockOrganisaatiot.helsinginKaupunki).flatMap(_.toKoulutustoimija)
      )
      "Oppilaitoksen oid muuttuu ja ei aikaisempaa organisaatio historiaa" in {
        resetFixtures
        verifyChange(change = { existing: AmmatillinenOpiskeluoikeus => existing.copy(oppilaitos = existing.oppilaitos.map(o => o.copy(oid = MockOrganisaatiot.omnia)), koulutustoimija = None)}) {
          verifyResponseStatusOk()
          lastOpiskeluoikeusByHetu(oppija).organisaatiohistoria should equal(Some(List(uusiOrganisaatioHistoria)))
        }
      }
      "Organisaatio historiaan ei voi tuoda dataa opiskeluoikeutta luotaessa" in {
        resetFixtures
        putOppija(Oppija(oppija, List(defaultOpiskeluoikeus.copy(organisaatiohistoria = Some(List(uusiOrganisaatioHistoria)))))) {
          verifyResponseStatusOk()
          lastOpiskeluoikeusByHetu(oppija).organisaatiohistoria should equal(None)
        }
      }
      "Uusi muutos lisätään vanhojen perään" in {
        resetFixtures
        val existing = lastOpiskeluoikeusByHetu(KoskiSpecificMockOppijat.organisaatioHistoria).asInstanceOf[AmmatillinenOpiskeluoikeus]
        putOppija(Oppija(KoskiSpecificMockOppijat.organisaatioHistoria, List(existing.copy(oppilaitos = Some(Oppilaitos(MockOrganisaatiot.winnova)), koulutustoimija = None)))) {
          lastOpiskeluoikeusByHetu(KoskiSpecificMockOppijat.organisaatioHistoria).organisaatiohistoria.get should equal(AmmatillinenExampleData.opiskeluoikeudenOrganisaatioHistoria :+ uusiOrganisaatioHistoria)
        }
      }
      "Organisaatiot eivät ole muuttuneet, vanha historia kopioidaan uuteen versioon" in {
        resetFixtures
        val existing = lastOpiskeluoikeusByHetu(KoskiSpecificMockOppijat.organisaatioHistoria).asInstanceOf[AmmatillinenOpiskeluoikeus]
        putOppija(Oppija(KoskiSpecificMockOppijat.organisaatioHistoria, List(existing.copy(ostettu = true)))) {
          lastOpiskeluoikeusByHetu(KoskiSpecificMockOppijat.organisaatioHistoria).organisaatiohistoria should equal(Some(
            AmmatillinenExampleData.opiskeluoikeudenOrganisaatioHistoria
          ))
        }
      }
    }

    def valmis(suoritus: AmmatillisenTutkinnonSuoritus) = suoritus.copy(
      vahvistus = ExampleData.vahvistus(päivä = date(2016, 10, 1), paikkakunta = Some(jyväskylä))
    )

    def verifyChange[T <: Opiskeluoikeus](original: T = defaultOpiskeluoikeus, user: UserWithPassword = defaultUser, user2: Option[UserWithPassword] = None, change: T => KoskeenTallennettavaOpiskeluoikeus)(block: => Unit) = {
      putOppija(Oppija(oppija, List(original)), authHeaders(user) ++ jsonContent) {
        verifyResponseStatusOk()
        val existing = lastOpiskeluoikeusByHetu(oppija).asInstanceOf[T]
        val updated: KoskeenTallennettavaOpiskeluoikeus = change(existing)
        putOppija(Oppija(oppija, List(updated)), authHeaders(user2.getOrElse(user)) ++ jsonContent) {
          block
        }
      }
    }
  }

  "Virheensieto" - {
    "Väärän tyyppinen request body" in {
      put("api/oppija", body = "\"hello\"", headers = authHeaders(paakayttaja) ++ jsonContent){
        verifyResponseStatus(400, ErrorMatcher.regex(KoskiErrorCategory.badRequest.validation.jsonSchema, ".*unexpectedType.*".r))
      }
    }
    "Väärän muotoinen hetu" in {
      putOppija(Oppija(oppija.copy(hetu = "291297"), List(defaultOpiskeluoikeus.copy(lähdejärjestelmänId = Some(winnovaLähdejärjestelmäId)))), headers = authHeaders(MockUsers.helsinginKaupunkiPalvelukäyttäjä) ++ jsonContent) {
        verifyResponseStatus(400, KoskiErrorCategory.badRequest.validation.henkilötiedot.hetu("Virheellinen muoto hetulla: 291297"))
      }
    }
    "Väärän muotoinen henkilö" in {
      val json =
        """{
            "henkilö": {
              "oid": "1.2.246.562.24.99999555555",
              "hetu": "270181-5263",
              "etunimet": "Kaisa",
              "kutsumanimi": "Kaisa",
              "sukunimi": "Koululainen",
              "äidinkieli": "väärän-muotoinen"
            },
            "opiskeluoikeudet": []
          }"""

      put("api/oppija", body = json, headers = authHeaders(paakayttaja) ++ jsonContent){
        verifyResponseStatus(400, ErrorMatcher.regex(KoskiErrorCategory.badRequest.validation.jsonSchema, ".*unexpectedType.*".r))
      }
    }

    "NULL merkki datassa" in {
      resetFixtures

      val opiskeluoikeus: AmmatillinenOpiskeluoikeus = defaultOpiskeluoikeus.copy(
        lisätiedot = Some(AmmatillinenExampleData.opiskeluoikeudenLisätiedot.copy(
          ulkomaanjaksot = Some(List(Ulkomaanjakso(
            alku = date(2012, 9, 1), loppu = None, maa = ExampleData.ruotsi, kuvaus = LocalizedString.finnish("kuv\u0000aus")
          ))))))

      putOppija(Oppija(oppija, List(opiskeluoikeus))) {
        verifyResponseStatusOk()
        lastOpiskeluoikeusByHetu(oppija)
          .lisätiedot.collect { case l: AmmatillisenOpiskeluoikeudenLisätiedot => l }.get
          .ulkomaanjaksot.get.head
          .kuvaus.get("fi") should equal("kuvaus")
      }
    }
  }
}
