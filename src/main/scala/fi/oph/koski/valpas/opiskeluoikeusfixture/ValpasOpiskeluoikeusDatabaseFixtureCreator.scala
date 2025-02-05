package fi.oph.koski.valpas.opiskeluoikeusfixture

import fi.oph.koski.config.KoskiApplication
import fi.oph.koski.fixture.DatabaseFixtureCreator
import fi.oph.koski.henkilo.OppijaHenkilö
import fi.oph.koski.json.JsonSerializer
import fi.oph.koski.schema._

class ValpasOpiskeluoikeusDatabaseFixtureCreator(application: KoskiApplication) extends DatabaseFixtureCreator(application, "opiskeluoikeus_valpas_fixture", "opiskeluoikeushistoria_valpas_fixture") {
  protected def oppijat = ValpasMockOppijat.defaultOppijat

  protected lazy val validatedOpiskeluoikeudet: List[(OppijaHenkilö, KoskeenTallennettavaOpiskeluoikeus)] = {
    defaultOpiskeluOikeudet.zipWithIndex.map { case ((henkilö, oikeus), index) =>
      timed(s"Validating fixture ${index}", 500) {
        validator.validateAsJson(Oppija(henkilö.toHenkilötiedotJaOid, List(oikeus))) match {
          case Right(oppija) => (henkilö, oppija.tallennettavatOpiskeluoikeudet.head)
          case Left(status) => throw new RuntimeException(
            s"Fixture insert failed for ${henkilö.etunimet} ${henkilö.sukunimi} with data ${JsonSerializer.write(oikeus)}: ${status}"
          )
        }
      }
    }
  }

  protected lazy val invalidOpiskeluoikeudet: List[(OppijaHenkilö, KoskeenTallennettavaOpiskeluoikeus)] = List()

  private def defaultOpiskeluOikeudet: List[(OppijaHenkilö, KoskeenTallennettavaOpiskeluoikeus)] = List(
    (ValpasMockOppijat.oppivelvollinenYsiluokkaKeskenKeväällä2021, ValpasOpiskeluoikeusExampleData.oppivelvollinenYsiluokkaKeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.eiOppivelvollinenSyntynytEnnen2004, ValpasOpiskeluoikeusExampleData.oppivelvollinenYsiluokkaKeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.päällekkäisiäOpiskeluoikeuksia, ValpasOpiskeluoikeusExampleData.oppivelvollinenVaihtanutKouluaMuttaOpiskeluoikeusMerkkaamattaOikein1),
    (ValpasMockOppijat.päällekkäisiäOpiskeluoikeuksia, ValpasOpiskeluoikeusExampleData.oppivelvollinenVaihtanutKouluaMuttaOpiskeluoikeusMerkkaamattaOikein2),
    (ValpasMockOppijat.lukioOpiskelija, ValpasOpiskeluoikeusExampleData.lukionOpiskeluoikeus),
    (ValpasMockOppijat.kasiluokkaKeskenKeväällä2021, ValpasOpiskeluoikeusExampleData.kasiluokkaKeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.kotiopetusMeneilläänOppija, ValpasOpiskeluoikeusExampleData.kotiopetusMeneilläänOpiskeluoikeus),
    (ValpasMockOppijat.kotiopetusMenneisyydessäOppija, ValpasOpiskeluoikeusExampleData.kotiopetusMenneisyydessäOpiskeluoikeus),
    (ValpasMockOppijat.eronnutOppija, ValpasOpiskeluoikeusExampleData.eronnutOpiskeluoikeusTarkastelupäivääEnnen),
    (ValpasMockOppijat.eronnutOppijaTarkastelupäivänä, ValpasOpiskeluoikeusExampleData.eronnutOpiskeluoikeusTarkastelupäivänä),
    (ValpasMockOppijat.eronnutOppijaTarkastelupäivänJälkeen, ValpasOpiskeluoikeusExampleData.eronnutOpiskeluoikeusTarkastelupäivänJälkeen),
    (ValpasMockOppijat.valmistunutYsiluokkalainen, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainen),
    (ValpasMockOppijat.valmistunutYsiluokkalainenJollaIlmoitus, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainen),
    (ValpasMockOppijat.luokalleJäänytYsiluokkalainen, ValpasOpiskeluoikeusExampleData.luokallejäänytYsiluokkalainen),
    (ValpasMockOppijat.luokallejäänytYsiluokkalainenJollaUusiYsiluokka, ValpasOpiskeluoikeusExampleData.luokallejäänytYsiluokkalainenJollaUusiYsiluokka),
    (ValpasMockOppijat.luokalleJäänytYsiluokkalainenVaihtanutKoulua, ValpasOpiskeluoikeusExampleData.luokallejäänytYsiluokkalainenVaihtanutKouluaEdellinen),
    (ValpasMockOppijat.luokalleJäänytYsiluokkalainenVaihtanutKoulua, ValpasOpiskeluoikeusExampleData.luokallejäänytYsiluokkalainenVaihtanutKouluaJälkimmäinen),
    (ValpasMockOppijat.luokalleJäänytYsiluokkalainenVaihtanutKouluaMuualta, ValpasOpiskeluoikeusExampleData.luokallejäänytYsiluokkalainenVaihtanutKouluaEdellinen2),
    (ValpasMockOppijat.luokalleJäänytYsiluokkalainenVaihtanutKouluaMuualta, ValpasOpiskeluoikeusExampleData.luokallejäänytYsiluokkalainenVaihtanutKouluaJälkimmäinen2),
    (ValpasMockOppijat.kasiinAstiToisessaKoulussaOllut, ValpasOpiskeluoikeusExampleData.kasiluokkaEronnutKeväällä2020Opiskeluoikeus),
    (ValpasMockOppijat.kasiinAstiToisessaKoulussaOllut, ValpasOpiskeluoikeusExampleData.pelkkäYsiluokkaKeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.kasiinAstiToisessaKoulussaOllutJollaIlmoitus, ValpasOpiskeluoikeusExampleData.kasiluokkaEronnutKeväällä2020Opiskeluoikeus),
    (ValpasMockOppijat.kasiinAstiToisessaKoulussaOllutJollaIlmoitus, ValpasOpiskeluoikeusExampleData.pelkkäYsiluokkaKeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.lukionAloittanut, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainen),
    (ValpasMockOppijat.lukionAloittanut, ValpasOpiskeluoikeusExampleData.lukionOpiskeluoikeusAlkaa2021Syksyllä),
    (ValpasMockOppijat.lukionLokakuussaAloittanut, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainen),
    (ValpasMockOppijat.lukionLokakuussaAloittanut, ValpasOpiskeluoikeusExampleData.lukionOpiskeluoikeusAlkaa2021Lokakuussa),
    (ValpasMockOppijat.oppivelvollinenMonellaOppijaOidillaMaster, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainen),
    (ValpasMockOppijat.oppivelvollinenMonellaOppijaOidillaToinen, ValpasOpiskeluoikeusExampleData.lukionOpiskeluoikeus),
    (ValpasMockOppijat.oppivelvollinenMonellaOppijaOidillaKolmas, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainenToinenKoulu),
    (ValpasMockOppijat.aapajoenPeruskoulustaValmistunut, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainenToinenKoulu),
    (ValpasMockOppijat.ennenLainRajapäivääPeruskoulustaValmistunut, ValpasOpiskeluoikeusExampleData.ennenLainRajapäivääToisestaKoulustaValmistunutYsiluokkalainen),
    (ValpasMockOppijat.ennenLainRajapäivääPeruskoulustaValmistunut, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainen),
    (ValpasMockOppijat.yli2kkAiemminPeruskoulustaValmistunut, ValpasOpiskeluoikeusExampleData.yli2kkAiemminPeruskoulustaValmistunut),
    (ValpasMockOppijat.oppivelvollinenAloittanutJaEronnutTarkastelupäivänJälkeen, ValpasOpiskeluoikeusExampleData.oppivelvollinenAloittanutJaEronnutTarkastelupäivänJälkeenOpiskeluoikeus),
    (ValpasMockOppijat.useampiYsiluokkaSamassaKoulussa, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainen),
    (ValpasMockOppijat.useampiYsiluokkaSamassaKoulussa, ValpasOpiskeluoikeusExampleData.kesäYsiluokkaKesken), // Tämä on vähän huono esimerkki, mutta varmistelee sitä, että homma toimii myös sitten, kun aletaan tukea nivelvaihetta, jossa nämä tapaukset voivat olla yleisempiä
    (ValpasMockOppijat.turvakieltoOppija, ValpasOpiskeluoikeusExampleData.oppivelvollinenYsiluokkaKeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.hakukohteidenHakuEpäonnistuu, ValpasOpiskeluoikeusExampleData.oppivelvollinenYsiluokkaKeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.kulosaarenYsiluokkalainen, ValpasOpiskeluoikeusExampleData.kulosaarelainenYsiluokkalainenOpiskeluoikeus),
    (ValpasMockOppijat.kulosaarenYsiluokkalainenJaJyväskylänLukiolainen, ValpasOpiskeluoikeusExampleData.kulosaarelainenYsiluokkalainenOpiskeluoikeus),
    (ValpasMockOppijat.kulosaarenYsiluokkalainenJaJyväskylänLukiolainen, ValpasOpiskeluoikeusExampleData.lukionOpiskeluoikeus),
    (ValpasMockOppijat.kulosaarenYsiluokkalainenJaJyväskylänNivelvaiheinen, ValpasOpiskeluoikeusExampleData.kulosaarelainenYsiluokkalainenOpiskeluoikeus),
    (ValpasMockOppijat.kulosaarenYsiluokkalainenJaJyväskylänNivelvaiheinen, ValpasOpiskeluoikeusExampleData.kymppiluokanOpiskeluoikeus),
    (ValpasMockOppijat.kulosaarenYsiluokkalainenJaJyväskylänEsikoululainen, ValpasOpiskeluoikeusExampleData.kulosaarelainenYsiluokkalainenOpiskeluoikeus),
    (ValpasMockOppijat.kulosaarenYsiluokkalainenJaJyväskylänEsikoululainen, ValpasOpiskeluoikeusExampleData.esiopetuksenOpiskeluoikeus),
    (ValpasMockOppijat.kahdenKoulunYsiluokkalainenJollaIlmoitus, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainen),
    (ValpasMockOppijat.kahdenKoulunYsiluokkalainenJollaIlmoitus, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainenToinenKoulu),
    (ValpasMockOppijat.lukionAineopinnotAloittanut, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainen),
    (ValpasMockOppijat.lukionAineopinnotAloittanut, ValpasOpiskeluoikeusExampleData.lukionAineopintojenOpiskeluoikeusAlkaa2021Syksyllä),
    (ValpasMockOppijat.oppivelvollinenMonellaOppijaOidillaJollaIlmoitusMaster, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainen),
    (ValpasMockOppijat.oppivelvollinenMonellaOppijaOidillaJollaIlmoitusToinen, ValpasOpiskeluoikeusExampleData.lukionOpiskeluoikeus),
    (ValpasMockOppijat.oppivelvollinenMonellaOppijaOidillaJollaIlmoitusKolmas, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainenToinenKoulu),
    (ValpasMockOppijat.lukionAloittanutJollaVanhaIlmoitus, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainen),
    (ValpasMockOppijat.lukionAloittanutJollaVanhaIlmoitus, ValpasOpiskeluoikeusExampleData.lukionOpiskeluoikeusAlkaa2021Syksyllä),
    (ValpasMockOppijat.lukionAloittanutJaLopettanutJollaIlmoituksia, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainen),
    (ValpasMockOppijat.lukionAloittanutJaLopettanutJollaIlmoituksia, ValpasOpiskeluoikeusExampleData.lukionOpiskeluoikeusAlkaaJaLoppuu2021Syksyllä),
    (ValpasMockOppijat.ammattikoulustaValmistunutOpiskelija, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainen),
    (ValpasMockOppijat.ammattikoulustaValmistunutOpiskelija, ValpasOpiskeluoikeusExampleData.ammattikouluValmistunutOpiskeluoikeus),
    (ValpasMockOppijat.eronnutMaaliskuussa17VuottaTäyttäväKasiluokkalainen, ValpasOpiskeluoikeusExampleData.eronnutOpiskeluoikeusEiYsiluokkaaKeväänAlussa),
    (ValpasMockOppijat.eronnutKeväänValmistumisJaksolla17VuottaTäyttäväKasiluokkalainen, ValpasOpiskeluoikeusExampleData.eronnutOpiskeluoikeusEiYsiluokkaaKeväänJaksolla),
    (ValpasMockOppijat.eronnutElokuussa17VuottaTäyttäväKasiluokkalainen, ValpasOpiskeluoikeusExampleData.eronnutOpiskeluoikeusEiYsiluokkaaElokuussa),
    (ValpasMockOppijat.valmistunutYsiluokkalainenVsop, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainenVsop),
    (ValpasMockOppijat.ysiluokkaKeskenVsop, ValpasOpiskeluoikeusExampleData.ysiluokkaKeskenVsop),
    (ValpasMockOppijat.valmistunutKasiluokkalainen, ValpasOpiskeluoikeusExampleData.valmistunutKasiluokkalainen),
    (ValpasMockOppijat.oppivelvollinenMonellaOppijaOidillaJollaIlmoitusMaster2, ValpasOpiskeluoikeusExampleData.lukionOpiskeluoikeus),
    (ValpasMockOppijat.oppivelvollinenMonellaOppijaOidillaJollaIlmoitusToinen2, ValpasOpiskeluoikeusExampleData.valmistunutYsiluokkalainen),
    (ValpasMockOppijat.ilmoituksenLisätiedotPoistettu, ValpasOpiskeluoikeusExampleData.oppivelvollinenYsiluokkaKeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.lukiostaValmistunutOpiskelija, ValpasOpiskeluoikeusExampleData.lukionOpiskeluoikeusValmistunut),
    (ValpasMockOppijat.ammattikouluOpiskelija, ValpasOpiskeluoikeusExampleData.ammattikouluOpiskeluoikeus),
    (ValpasMockOppijat.kaksoistutkinnostaValmistunutOpiskelija, ValpasOpiskeluoikeusExampleData.ammattikouluValmistunutOpiskeluoikeus),
    (ValpasMockOppijat.kaksoistutkinnostaValmistunutOpiskelija, ValpasOpiskeluoikeusExampleData.lukionOpiskeluoikeusValmistunut),
    (ValpasMockOppijat.nivelvaiheestaValmistunutOpiskelija, ValpasOpiskeluoikeusExampleData.valmistunutKymppiluokkalainen),
    (ValpasMockOppijat.oppivelvollisuusKeskeytetty, ValpasOpiskeluoikeusExampleData.oppivelvollinenYsiluokkaKeskenKeväällä2021Opiskeluoikeus),
    (ValpasMockOppijat.oppivelvollisuusKeskeytettyToistaiseksi, ValpasOpiskeluoikeusExampleData.oppivelvollinenYsiluokkaKeskenKeväällä2021Opiskeluoikeus),
  )
}
